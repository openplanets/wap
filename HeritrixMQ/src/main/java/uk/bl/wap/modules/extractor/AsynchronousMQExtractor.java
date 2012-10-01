package uk.bl.wap.modules.extractor;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.modules.CrawlURI;
import org.archive.modules.extractor.ContentExtractor;
import org.springframework.beans.factory.annotation.Autowired;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

/**
 * Sends a message consisting of a URL and directory to a queue. This URL can be
 * processed asynchronously and discovered URLs written back to the Action directory
 * for Heritrix to process.
 * 
 * <bean id="extractorMq" class="uk.bl.wap.modules.extractor.AsynchronousMQExtractor">
 * 	<property name="host" value="localhost" />
 * 	<property name="routingKey" value="phantomjs" />
 * 	<property name="queue" value="phantomjs" />
 * 	<property name="outputPath" value="/opt/heritrix/jobs/<job_ref>/action/" />
 * </bean>
 * 
 * @author rcoram
 * 
 */

public class AsynchronousMQExtractor extends ContentExtractor {
	private static Logger logger = Logger.getLogger( AsynchronousMQExtractor.class.getName() );
	private final static String SLASH_PAGE = "^https?://?[^/]+/$";
	private Pattern pattern;
	private Connection conn;
	private Channel channel;
	private ConnectionFactory factory;

	{
		setHost( "localhost" );
	}
	public String getHost() {
		return ( String ) kp.get( "host" );
	}
	@Autowired
	public void setHost( String host ) {
		kp.put( "host", host );
	}

	{
		setRoutingKey( "phantomjs" );
	}
	public String getRoutingKey() {
		return ( String ) kp.get( "routingKey" );
	}
	@Autowired
	public void setRoutingKey( String key ) {
		kp.put( "routingKey", key );
	}

	{
		setQueue( "phantomjs" );
	}
	public String getQueue() {
		return ( String ) kp.get( "routingKey" );
	}
	@Autowired
	public void setQueue( String queue ) {
		kp.put( "queue", queue );
	}

	public String getOutputPath() {
		return ( String ) kp.get( "outputPath" );
	}
	@Autowired
	public void setOutputPath( String output ) {
		kp.put( "outputPath", output );
	}

	public AsynchronousMQExtractor() {
		this.pattern = Pattern.compile( SLASH_PAGE );
		try {
			this.factory = new ConnectionFactory();
			this.factory.setHost( this.getHost() );
			this.conn = factory.newConnection();
			this.channel = conn.createChannel();
			this.channel.queueDeclare( this.getQueue(), false, false, false, null );
		} catch( Exception e ) {
			logger.warning( e.getMessage() );
		}
	}

	@Override
	protected boolean innerExtract( CrawlURI curi ) {
		String message = curi.getURI() + "|" + this.getOutputPath();
		try {
			logger.info( "Sending " + message + " to " + this.getQueue() );
			channel.basicPublish( "", this.getRoutingKey(), true, false, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes() );
		} catch( Exception e ) {
			logger.warning( e.getMessage() );
		}
		return false;
	}

	@Override
	protected boolean shouldExtract( CrawlURI curi ) {
		if( curi.isSeed() )
			return true;
		Matcher matcher = this.pattern.matcher( curi.getURI() );
		return matcher.matches();
	}
}
