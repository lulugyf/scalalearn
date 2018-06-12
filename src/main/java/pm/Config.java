package pm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/*
format:
PT.web=<localport>,<remote_host>,<remote_port>
PT.??=

threadPoolCapacity = 10
byteBufferCapacity = 2048

 */

public class Config {

    private static final Logger logger = Logger.getLogger("Config");

    private static final String PROPERTIES_FILE_NAME = "/proxy.properties";
    private static final String USE_DEFAULT_VALUE_MESSAGE = "Property %s is missing in " + PROPERTIES_FILE_NAME + ". Used default value \"%s\" instead.";

    private static final String THREAD_POOL_CAPACITY_KEY = "threadPoolCapacity";
    private static final int THREAD_POOL_CAPACITY_DEFAULT_VALUE = 10;

    private static final String BYTE_BUFFER_CAPACITY_KEY = "byteBufferCapacity";
    private static final int BYTE_BUFFER_CAPACITY_DEFAULT_VALUE = 1024;

    private static final Config instance = new Config();

    private Set<ServiceDescriptor> serviceDescriptors;
    private Properties pp = new Properties();

    private Config() {
        serviceDescriptors = new HashSet<>();
        try {
            String fname = System.getProperty("CONF_FILE");
            if(fname == null)
                fname = PROPERTIES_FILE_NAME;
            fname = Config.class.getClassLoader().getResource(fname).getPath();
            logger.info("Using conf file: "+fname );

            // Assume that properties might not be in order
            BufferedReader bf = new BufferedReader(new FileReader(fname));
            String line = null;
            while( (line = bf.readLine()) != null){
                line = line.trim();
                if(!line.startsWith("PT.")) {
                    int p = line.indexOf('=');
                    if(p > 0){
                        pp.setProperty(line.substring(0, p).trim(), line.substring(p+1).trim());
                    }
                    continue;
                }
                int p = line.indexOf('=');
                if(p > 0){
                    String pname = line.substring(0, p).trim();
                    String pvalue = line.substring(p+1).trim();
                    String serviceid = pname.substring(3);
                    ServiceDescriptor service = new ServiceDescriptor(serviceid);
                    serviceDescriptors.add(service);
                    String[] vals = pvalue.split("\\,");

                    service.setLocalPort(Integer.parseInt(vals[0]));
                    service.setRemoteHost(vals[1]);
                    service.setRemotePort(Integer.parseInt(vals[2]));
                }
            }
            bf.close();
        } catch (IOException e) {
            throw new RuntimeException("Cant' load properties file", e);
        }
    }

    public static Config getInstance() {
        return instance;
    }

    public Set<ServiceDescriptor> getServiceDescriptors() {
        return serviceDescriptors;
    }


    public int getThreadPoolCapacity() {
        return getPropertyOrDefaultWithNotification(THREAD_POOL_CAPACITY_KEY, THREAD_POOL_CAPACITY_DEFAULT_VALUE);
    }

    public int getByteBufferCapacity() {
        return getPropertyOrDefaultWithNotification(BYTE_BUFFER_CAPACITY_KEY, BYTE_BUFFER_CAPACITY_DEFAULT_VALUE);
    }

    private int getPropertyOrDefaultWithNotification(String key, int defaultValue) {
        String value = pp.getProperty(key);

        if (value == null) {
            //logger.info(String.format(USE_DEFAULT_VALUE_MESSAGE, key, defaultValue));
            return defaultValue;
        }

        return Integer.valueOf(value);
    }
}
