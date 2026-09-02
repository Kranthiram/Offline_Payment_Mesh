package com.demo.upi_offline_mesh.model;

public class MeshPacket {
    private String packetId;
    private int ttl;
    private long createdAt;
    private String ciphertext;

    public MeshPacket(){};

    public MeshPacket(String packetId,int ttl,long createdAt,String ciphertext){
        this.packetId = packetId;
        this.ttl = ttl;
        this.createdAt = createdAt;
        this.ciphertext = ciphertext;
    }

    public String getPackedId(){
        return packetId;
    }
    public void setPacketId(String packetId){
        this.packetId = packetId;
    }
    public int getTtl(){
        return ttl;
    }
    public void setTtl(int ttl){
        this.ttl = ttl;
    }
    public long getCreatedAt(){
        return createdAt;
    }
    public void setCreatedAt(long createdAt){
        this.createdAt = createdAt;
    }
    public String getCiphertext(){
        return ciphertext;
    }
    public void setCiphertext(String ciphertext){
        this.ciphertext = ciphertext;
    }
}
