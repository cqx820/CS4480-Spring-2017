#!usr/bin/python
from ryu.base import app_manager
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER, MAIN_DISPATCHER, DEAD_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_3
from ryu.lib.packet import packet
from ryu.lib.packet import ethernet
from ryu.lib.packet import ether_types
from ryu.lib.packet import arp
from ryu.lib.packet import ipv4


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
        eth = pkt.get_protocol(ethernet.ethernet)
        if not eth:
            return

        arp_pkt = pkt.get_protocol(arp.arp)
        # self.logger.info("!!!!!!@@@@@@@@@@@!!!!!!!!!!!!!!!!!!!!!packet-out %s" % (pkt,))
        self.logger.info("!!!!!Received src ip is: %s  and dst ip is %s  and src mac is %s", arp_pkt.src_ip,
                         arp_pkt.dst_ip, eth.src)
        h_src_ip = arp_pkt.src_ip
        h_dst_ip = arp_pkt.dst_ip
        h_src_mac = eth.src
        self.logger.info("Src mac is %s ", h_src_mac)

        if h_src_ip == '10.0.0.5':
            s_dst_mac = ''
            if h_dst_ip == '10.0.0.1':
                s_dst_mac = '00:00:00:00:00:01'
            elif h_dst_ip == '10.0.0.3':
                s_dst_mac = '00:00:00:00:00:03'
            self.logger.info("dst mac is %s ", s_dst_mac)
            # match = parser.OFPMatch(in_port=in_port, ipv4_dst=h_dst_ip, eth_type=0x800)
            p = packet.Packet()
            arp_eth = ethernet.ethernet(ethertype=eth.ethertype, dst=h_src_mac, src=s_dst_mac)
            arp_arp = arp.arp(opcode=arp.ARP_REPLY, src_mac=s_dst_mac, src_ip=h_dst_ip,
                              dst_mac=h_src_mac, dst_ip=h_src_ip)
            p.add_protocol(arp_eth)
            p.add_protocol(arp_arp)
            # p.add_protocol(ethernet.ethernet(ethertype=eth.ethertype, dst=h_src_mac, src=s_dst_mac))
            # p.add_protocol(
            #     arp.arp(1, 0x0800, 6, 4, opcode=arp.ARP_REPLY, src_mac=s_dst_mac, src_ip=h_dst_ip, dst_mac=h_src_mac,
            #             dst_ip=h_src_ip))
            p.serialize()
            self.logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!packet-out %s" % (p,))
            p1 = packet.Packet()
            p1.add_protocol(ethernet.ethernet(ethertype=eth.ethertype, dst=s_dst_mac, src=h_src_mac))
            p1.add_protocol(
                arp.arp(opcode=arp.ARP_REPLY, src_mac=h_src_mac, src_ip=h_src_ip, dst_mac=s_dst_mac, dst_ip=h_dst_ip))
            p1.serialize()
            # p_data = p.data


            # match = parser.OFPMatch(eth_type=0x0806, ip_proto=5)
            # actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER, ofproto.OFPCML_NO_BUFFER)]
            # self.add_flow(datapath, 20, match, actions)

            # data = p_data
            # out = parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id, in_port=in_port,
            #                          actions=actions, data=data)

            # match = parser.OFPMatch(in_port=in_port, ipv4_dst=h_dst_ip, eth_type=0x800)

            arp_actions = [parser.OFPActionOutput(in_port)]
            arp_out = parser.OFPPacketOut(datapath=datapath, buffer_id=ofproto.OFP_NO_BUFFER,
                                          in_port=ofproto.OFPP_CONTROLLER, actions=arp_actions, data=p.data)
            datapath.send_msg(arp_out)
            arp1_actions = [parser.OFPActionOutput(5)]
            arp1_out = parser.OFPPacketOut(datapath=datapath, buffer_id=ofproto.OFP_NO_BUFFER,
                                           in_port=ofproto.OFPP_CONTROLLER, actions=arp1_actions, data=p1.data)
            datapath.send_msg(arp1_out)
            self.logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!packet out!!!")

        elif h_src_ip == '10.0.0.1' or h_src_ip == '10.0.0.3':
            s_dst_mac = '00:00:00:00:00:05'
            match = parser.OFPMatch(in_port=in_port, ipv4_dst=h_dst_ip, eth_type=0x800)
            output_port = 5
            # dpid = datapath.id
            actions = [parser.OFPActionSetField(ipv4_dst='10.0.0.5'),
                       parser.OFPActionOutput(output_port)]
            self.logger.info("Action is %s " % (actions,))
            self.add_flow(datapath, 1, match, actions)

            match = parser.OFPMatch(in_port=output_port, ipv4_src='10.0.0.5', ipv4_dst=h_src_ip, eth_type=0x800)
            actions = [parser.OFPActionSetField(ipv4_src='10.0.0.10'),
                       parser.OFPActionOutput(in_port)]
            self.add_flow(datapath, 1, match, actions)
            data = msg.data
            # self.logger.info("@@@@@@@@@@@@@@@@@@@@@@@data is %s " % (data, ))
            out = parser.OFPPacketOut(datapath=datapath, buffer_id=ofproto.OFP_NO_BUFFER, in_port=in_port,
                                      actions=actions, data=data)
            datapath.send_msg(out)
            self.logger.info("dst mac is %s ", s_dst_mac)
            # match = parser.OFPMatch(in_port=in_port, ipv4_dst=h_dst_ip, eth_type=0x800)
            p = packet.Packet()
            arp_eth = ethernet.ethernet(ethertype=eth.ethertype, dst=h_src_mac, src=s_dst_mac)
            arp_arp = arp.arp(opcode=arp.ARP_REPLY, src_mac=s_dst_mac, src_ip=h_dst_ip,
                              dst_mac=h_src_mac, dst_ip=h_src_ip)
            p.add_protocol(arp_eth)
            p.add_protocol(arp_arp)
            # p.add_protocol(ethernet.ethernet(ethertype=eth.ethertype, dst=h_src_mac, src=s_dst_mac))
            # p.add_protocol(
            #     arp.arp(1, 0x0800, 6, 4, opcode=arp.ARP_REPLY, src_mac=s_dst_mac, src_ip=h_dst_ip, dst_mac=h_src_mac,
            #             dst_ip=h_src_ip))
            p.serialize()
            self.logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!packet-out %s" % (p,))
            p1 = packet.Packet()
            p1.add_protocol(ethernet.ethernet(ethertype=eth.ethertype, dst=s_dst_mac, src=h_src_mac))
            p1.add_protocol(
                arp.arp(opcode=arp.ARP_REPLY, src_mac=h_src_mac, src_ip=h_src_ip, dst_mac=s_dst_mac, dst_ip=h_dst_ip))
            p1.serialize()
            # p_data = p.data


            # match = parser.OFPMatch(eth_type=0x0806, ip_proto=5)
            # actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER, ofproto.OFPCML_NO_BUFFER)]
            # self.add_flow(datapath, 20, match, actions)

            # data = p_data
            # out = parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id, in_port=in_port,
            #                          actions=actions, data=data)

            # match = parser.OFPMatch(in_port=in_port, ipv4_dst=h_dst_ip, eth_type=0x800)

            arp_actions = [parser.OFPActionOutput(in_port)]
            arp_out = parser.OFPPacketOut(datapath=datapath, buffer_id=ofproto.OFP_NO_BUFFER,
                                          in_port=ofproto.OFPP_CONTROLLER, actions=arp_actions, data=p.data)
            datapath.send_msg(arp_out)
            arp1_actions = [parser.OFPActionOutput(5)]
            arp1_out = parser.OFPPacketOut(datapath=datapath, buffer_id=ofproto.OFP_NO_BUFFER,
                                           in_port=ofproto.OFPP_CONTROLLER, actions=arp1_actions, data=p1.data)
            datapath.send_msg(arp1_out)
            self.logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!packet out!!!")

            # data = p.data

            # elif h_src_ip == '10.0.0.5':	 # cpy_pkt = packet.Packet(pkt.data[:])

            self.logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!@@@@@@@@@@@@@@@@@@@@@Port is %d, ipv4_dst is %s", in_port,
                             h_dst_ip)


        elif h_src_ip == '10.0.0.6':
            s_dst_mac = ''
            if h_dst_ip == '10.0.0.2':
                s_dst_mac = '00:00:00:00:00:02'
            elif h_dst_ip == '10.0.0.4':
                s_dst_mac = '00:00:00:00:00:04'
            self.logger.info("dst mac is %s ", s_dst_mac)
            # match = parser.OFPMatch(in_port=in_port, ipv4_dst=h_dst_ip, eth_type=0x800)
            p = packet.Packet()
            arp_eth = ethernet.ethernet(ethertype=eth.ethertype, dst=h_src_mac, src=s_dst_mac)
            arp_arp = arp.arp(opcode=arp.ARP_REPLY, src_mac=s_dst_mac, src_ip=h_dst_ip,
                              dst_mac=h_src_mac, dst_ip=h_src_ip)
            p.add_protocol(arp_eth)
            p.add_protocol(arp_arp)
            # p.add_protocol(ethernet.ethernet(ethertype=eth.ethertype, dst=h_src_mac, src=s_dst_mac))
            # p.add_protocol(
            #     arp.arp(1, 0x0800, 6, 4, opcode=arp.ARP_REPLY, src_mac=s_dst_mac, src_ip=h_dst_ip, dst_mac=h_src_mac,
            #             dst_ip=h_src_ip))
            p.serialize()
            self.logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!packet-out %s" % (p,))
            p1 = packet.Packet()
            p1.add_protocol(ethernet.ethernet(ethertype=eth.ethertype, dst=s_dst_mac, src=h_src_mac))
            p1.add_protocol(
                arp.arp(opcode=arp.ARP_REPLY, src_mac=h_src_mac, src_ip=h_src_ip, dst_mac=s_dst_mac,
                        dst_ip=h_dst_ip))
            p1.serialize()
            # p_data = p.data
            # match = parser.OFPMatch(eth_type=0x0806, ip_proto=5)
            # actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER, ofproto.OFPCML_NO_BUFFER)]
            # self.add_flow(datapath, 20, match, actions)

            # data = p_data
            # out = parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id, in_port=in_port,
            #                          actions=actions, data=data)

            # match = parser.OFPMatch(in_port=in_port, ipv4_dst=h_dst_ip, eth_type=0x800)

            arp_actions = [parser.OFPActionOutput(in_port)]
            arp_out = parser.OFPPacketOut(datapath=datapath, buffer_id=ofproto.OFP_NO_BUFFER,
                                          in_port=ofproto.OFPP_CONTROLLER, actions=arp_actions, data=p.data)
            datapath.send_msg(arp_out)
            arp1_actions = [parser.OFPActionOutput(6)]
            arp1_out = parser.OFPPacketOut(datapath=datapath, buffer_id=ofproto.OFP_NO_BUFFER,
                                           in_port=ofproto.OFPP_CONTROLLER, actions=arp1_actions, data=p1.data)
            datapath.send_msg(arp1_out)
            self.logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!packet out!!!")

        elif h_src_ip == '10.0.0.2' or h_src_ip == '10.0.0.4':
            s_dst_mac = '00:00:00:00:00:06'
            match = parser.OFPMatch(in_port=in_port, ipv4_dst=h_dst_ip, eth_type=0x800)
            output_port = 6
            # dpid = datapath.id
            actions = [parser.OFPActionSetField(ipv4_dst='10.0.0.6'),
                       parser.OFPActionOutput(output_port)]
            self.logger.info("Action is %s " % (actions,))
            self.add_flow(datapath, 1, match, actions)

            match = parser.OFPMatch(in_port=output_port, ipv4_src='10.0.0.6', ipv4_dst=h_src_ip, eth_type=0x800)
            actions = [parser.OFPActionSetField(ipv4_src='10.0.0.10'),
                       parser.OFPActionOutput(in_port)]
            self.add_flow(datapath, 1, match, actions)
            data = msg.data
            # self.logger.info("@@@@@@@@@@@@@@@@@@@@@@@data is %s " % (data, ))
            out = parser.OFPPacketOut(datapath=datapath, buffer_id=ofproto.OFP_NO_BUFFER, in_port=in_port,
                                      actions=actions, data=data)
            datapath.send_msg(out)
            self.logger.info("dst mac is %s ", s_dst_mac)
            # match = parser.OFPMatch(in_port=in_port, ipv4_dst=h_dst_ip, eth_type=0x800)
            p = packet.Packet()
            arp_eth = ethernet.ethernet(ethertype=eth.ethertype, dst=h_src_mac, src=s_dst_mac)
            arp_arp = arp.arp(opcode=arp.ARP_REPLY, src_mac=s_dst_mac, src_ip=h_dst_ip,
                              dst_mac=h_src_mac, dst_ip=h_src_ip)
            p.add_protocol(arp_eth)

            p.add_protocol(arp_arp)
            # p.add_protocol(ethernet.ethernet(ethertype=eth.ethertype, dst=h_src_mac, src=s_dst_mac))
            # p.add_protocol(
            #     arp.arp(1, 0x0800, 6, 4, opcode=arp.ARP_REPLY, src_mac=s_dst_mac, src_ip=h_dst_ip, dst_mac=h_src_mac,
            #             dst_ip=h_src_ip))
            p.serialize()
            self.logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!packet-out %s" % (p,))
            p1 = packet.Packet()
            p1.add_protocol(ethernet.ethernet(ethertype=eth.ethertype, dst=s_dst_mac, src=h_src_mac))
            p1.add_protocol(
                arp.arp(opcode=arp.ARP_REPLY, src_mac=h_src_mac, src_ip=h_src_ip, dst_mac=s_dst_mac,
                        dst_ip=h_dst_ip))
            p1.serialize()
            # p_data = p.data


            # match = parser.OFPMatch(eth_type=0x0806, ip_proto=5)
            # actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER, ofproto.OFPCML_NO_BUFFER)]
            # self.add_flow(datapath, 20, match, actions)

            # data = p_data
            # out = parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id, in_port=in_port,
            #                          actions=actions, data=data)

            # match = parser.OFPMatch(in_port=in_port, ipv4_dst=h_dst_ip, eth_type=0x800)

            arp_actions = [parser.OFPActionOutput(in_port)]
            arp_out = parser.OFPPacketOut(datapath=datapath, buffer_id=ofproto.OFP_NO_BUFFER,
                                          in_port=ofproto.OFPP_CONTROLLER, actions=arp_actions, data=p.data)
            datapath.send_msg(arp_out)
            arp1_actions = [parser.OFPActionOutput(6)]
            arp1_out = parser.OFPPacketOut(datapath=datapath, buffer_id=ofproto.OFP_NO_BUFFER,
                                           in_port=ofproto.OFPP_CONTROLLER, actions=arp1_actions, data=p1.data)
            datapath.send_msg(arp1_out)
            self.logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!packet out!!!")

            # data = p.data

            # elif h_src_ip == '10.0.0.5':	 # cpy_pkt = packet.Packet(pkt.data[:])

            self.logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!@@@@@@@@@@@@@@@@@@@@@Port is %d, ipv4_dst is %s", in_port,
                             h_dst_ip)
