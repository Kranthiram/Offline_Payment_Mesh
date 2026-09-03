package com.demo.upi_offline_mesh.crypto;

import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Component
public class ServerKeyHolder {
    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public ServerKeyHolder(){
        try{
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair  = generator.generateKeyPair();

            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        } catch(NoSuchAlgorithmException e){
            throw new IllegalStateException("RSA Algorithm not available",e);
        }
    }
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }
}
