package com.hkshenoy.jaltantraloopsb.security;

import com.hkshenoy.jaltantraloopsb.structs.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class NetworkStorageServiceImpl implements NetworkStorageService {
    private WaterNetworkRepository waterNetworkRepository;

    @Autowired
    public NetworkStorageServiceImpl(WaterNetworkRepository waterNetworkRepository) {
        this.waterNetworkRepository = waterNetworkRepository;
    }


    @Override
    public void saveNetwork(Network network, boolean solve, String type, Long user_id) {
        WaterNetwork waterNetwork = new WaterNetwork();

        waterNetwork.setGeneral(network.general);
        waterNetwork.setType(type);
        waterNetwork.setNodes(network.nodes);
        waterNetwork.setPipes(network.pipes);
        waterNetwork.setCommercialPipes(network.commercialPipes);
        waterNetwork.setEsrCost(network.esrCost);
        waterNetwork.setValves(network.valves);
        waterNetwork.setPumpGeneral(network.pumpGeneral);
        waterNetwork.setPumpManual(network.pumpManual);
        waterNetwork.setEsrGeneral(network.esrGeneral);
        waterNetwork.setUserid(user_id);
        waterNetworkRepository.save(waterNetwork);
    }
}
