import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

public class KafkaExample {
    private final String topic;
    private final Properties props;

    public KafkaExample(String brokers, String username, String password) {
        this.topic = username + "-default";

        String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
        String jaasCfg = String.format(jaasTemplate, username, password);

        String serializer = StringSerializer.class.getName();
        String deserializer = StringDeserializer.class.getName();
        props = new Properties();
        props.put("bootstrap.servers", brokers);
        props.put("group.id", username + "-consumer");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("auto.offset.reset", "earliest");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", deserializer);
        props.put("value.deserializer", deserializer);
        props.put("key.serializer", serializer);
        props.put("value.serializer", serializer);
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "SCRAM-SHA-256");
        props.put("sasl.jaas.config", jaasCfg);
    }

    public void consume() {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topic));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(1000);
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("%s [%d] offset=%d, key=%s, value=\"%s\"\n",
								  record.topic(), record.partition(),
								  record.offset(), record.key(), record.value());
			}
        }
    }

    public void produce() {
        Thread one = new Thread() {
            public void run() {
                try {
                    Producer<String, String> producer = new KafkaProducer<>(props);
                    int i = 0;
                    while(true) {
                        Date d = new Date();
                        producer.send(new ProducerRecord<>(topic, Integer.toString(i), d.toString()));
                        Thread.sleep(1000);
                        i++;
                    }
                } catch (InterruptedException v) {
                    System.out.println(v);
                }
            }
        };
        one.start();
    }

    public static void main(String[] args) {
		String brokers = System.getenv("CLOUDKARAFKA_BROKERS");
		String username = System.getenv("CLOUDKARAFKA_USERNAME");
		String password = System.getenv("CLOUDKARAFKA_PASSWORD");
		KafkaExample c = new KafkaExample(brokers, username, password);
        c.produce();
        c.consume();
    }
}
