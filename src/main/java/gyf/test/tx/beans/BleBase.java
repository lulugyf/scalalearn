package gyf.test.tx.beans;

public class BleBase {
    private long ble_id;
    private String addr_ip;
    private int addr_port;

    public long getBle_id() {
        return ble_id;
    }

    public void setBle_id(long ble_id) {
        this.ble_id = ble_id;
    }

    public String getAddr_ip() {
        return addr_ip;
    }

    public void setAddr_ip(String addr_ip) {
        this.addr_ip = addr_ip;
    }

    public int getAddr_port() {
        return addr_port;
    }

    public void setAddr_port(int addr_port) {
        this.addr_port = addr_port;
    }
}
