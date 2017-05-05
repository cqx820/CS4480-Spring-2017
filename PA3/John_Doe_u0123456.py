#!usr/bin/python
from ryu.base import app_manager
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER, MAIN_DISPATCHER, DEAD_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_3
from ryu.lib.packet import packet
from ryu.lib.packet import ethernet, tcp
from ryu.lib.packet import ether_types


class LoadBalance(app_manager.RyuApp):
    OFP_VERSIONS = [ofproto_v1_3.OFP_VERSION]

    def __init__(self, *args, **kwargs):
        super(LoadBalance, self).__init__(*args, **kwargs)
        self.mac_to_port = {}
        self.datapaths = {}

    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def switch_features_handler(self, ev):
        datapath = ev.msg.datapath
        parser = datapath.ofproto_parser
        ofproto = datapath.ofproto
        match = parser.OFPMatch()
        actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER,
                                          ofproto.OFPCML_NO_BUFFER)]
        self.add_flow(datapath, 0, match, actions)

    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def add_tcp_proto(self, ev):
        datapath = ev.msg.datapath
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser
        match = parser.OFPMatch(eth_type=0x0800, ip_proto=6)
        # https://en.wikipedia.org/wiki/EtherType
        # https://en.wikipedia.org/wiki/List_of_IP_protocol_numbers

        actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER, ofproto.OFPCML_NO_BUFFER)]

        self.add_flow(datapath, 10, match, actions)

    def add_flow(self, datapath, priority, match, actions, buffer_id=None):
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser

        instructions = [parser.OFPInstructionActions(ofproto.OFPIT_APPLY_ACTIONS, actions)]

        if buffer_id:
            mod = parser.OFPFlowMod(datapath=datapath, buffer_id=buffer_id,
                                    priority=priority, match=match, instructions=instructions)

        else:
            mod = parser.OFPFlowMod(datapath=datapath, priority=priority,
                                    match=match, instructions=instructions)

        datapath.send_msg(mod)

    @set_ev_cls(ofp_event.EventOFPPacketIn, MAIN_DISPATCHER)
    def _packet_in_handler(self, ev):

        msg = ev.msg
        datapath = msg.datapath
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser

        in_port = msg.match['in_port']
        pkt = packet.Packet(msg.data)
        eth = pkt.get_protocols(ethernet.ethernet)[0]

        if pkt.get_protocol(tcp.tcp) and (eth.dst == "00:00:00:00:00:05" or eth.dst == "00:00:00:00:00:06"):

            tc = pkt.get_protocols(tcp.tcp)[0]
            dstport = tc.dst_port
            srcport = tc.src_port
            dst = eth.dst
            src = eth.src

            dpid = datapath.id

            self.logger.info("Received TCP packet \n srcport: %d \t\tdstport: %d", srcport, dstport)

            match = parser.OFPMatch(tcp_src=srcport, tcp_dst=dstport, in_port=in_port, ip_proto=6, eth_type=0x0800)

            if eth.dst == "00:00:00:00:00:05":

                self.logger.info("forwarding packet to h5")
                actions = [parser.OFPActionOutput(5)]
                self.add_flow(datapath, 15, match, actions)
                match = parser.OFPMatch(tcp_src=srcport, tcp_dst=dstport, in_port=5, ip_proto=6, eth_type=0x0800)
                actions = [parser.OFPActionOutput(in_port)]
                self.add_flow(datapath, 15, match, actions)
            elif eth.dst == "00:00:00:00:00:06":

                self.logger.info("forwarding packet to h6")
                actions = [parser.OFPActionOutput(6)]
                self.add_flow(datapath, 15, match, actions)
                match = parser.OFPMatch(tcp_src=srcport, tcp_dst=dstport, in_port=6, ip_proto=6, eth_type=0x0800)
                actions = [parser.OFPActionOutput(in_port)]
                self.add_flow(datapath, 15, match, actions)

            data = None
            if msg.buffer_id == ofproto.OFP_NO_BUFFER:
                data = msg.data
            out = parser.OFPacketOut(datapath=datapath, buffer_id=msg.buffer_id, in_port=in_port,
                                     actions=actions, data=data)

            datapath.send_msg(out)
        else:
            dst = eth.dst
            src = eth.src

            dpid = datapath.id
            self.mac_to_port.setdefault(dpid, {})
            self.logger.info("packet %s %s %s %s", dpid, src, dst, in_port)

            self.mac_to_port[dpid][src] = in_port
            if dst in self.mac_to_port[dpid]:
                out_port = self.mac_to_port[dpid][dst]
            else:
                out_port = ofproto.OFPP_FLOOD

            actions = [parser.OFPActionOutput(out_port)]

            if out_port != ofproto.OFPP_FLOOD:
		match = parser.OFPMatch(in_port=in_port, eth_dst=dst)
	#	if msg.buffer_id != ofproto.OFP_NO_BUFFER:
		self.add_flow(datapath, 1, match, actions)
				
            data = None
            if msg.buffer_id == ofproto.OFP_NO_BUFFER:
                data = msg.data

            out = parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id, in_port=in_port,
                                      actions=actions, data=data)

            datapath.send_msg(out)

    @set_ev_cls(ofp_event.EventOFPStateChange, [MAIN_DISPATCHER, DEAD_DISPATCHER])
    def _state_change_handler(self, ev):
        datapath = ev.datapath

        if ev.state == MAIN_DISPATCHER:
            if not datapath.id in self.datapaths:
                #                self.logger.debug('register datapath: %016x', datapath.id)
                self.datapaths[datapath.id] = datapath
        elif ev.state == DEAD_DISPATCHER:
            if datapath.id in self.datapaths:
                #               self.logger.debug('unregister datapath: %016x', datapath.id)
                del self.datapaths[datapath.id]
