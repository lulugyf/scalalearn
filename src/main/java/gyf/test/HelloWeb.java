package gyf.test;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;

public class HelloWeb{

    private static ContextHandler addContext(String contextPath, Handler handler) {
        ContextHandler context = new ContextHandler(contextPath);
        context.setHandler(handler);
        return context;
    }

    public static void main( String[] args ) throws Exception
    {
        Server server = new Server(8080);

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{ "index.html" });
        resource_handler.setResourceBase("resource");

        byte[] bytes = new byte[100];

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] {
                addContext("/resource", resource_handler),
                addContext("/", new HelloHandler("Root Hello")),
                addContext("/fr", new HelloHandler("Bonjoir")),
                addContext("/it", new HelloHandler("Bongiorno")),
                 });

        server.setHandler(contexts);

        server.start();
        server.join();
    }

    public void test() {
        org.mybatis.spring.mapper.MapperScannerConfigurer v;
    }
}
