package com.hkshenoy.jaltantraloopsb.security;


import org.springframework.stereotype.Service;
import com.hkshenoy.jaltantraloopsb.structs.*;

@Service
public interface NetworkStorageService {
    void saveNetwork(Network network, boolean solve, String type);
}
