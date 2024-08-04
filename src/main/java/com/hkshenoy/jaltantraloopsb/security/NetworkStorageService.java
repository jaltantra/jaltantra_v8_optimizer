package com.hkshenoy.jaltantraloopsb.security;

import org.apache.commons.math3.ml.neuralnet.Network;

public interface NetworkStorageService {
    void saveNetwork(Network network, boolean solve, String type);
}
