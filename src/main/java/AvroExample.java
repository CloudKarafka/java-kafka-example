import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import java.util.Properties;

public class AvroExample {

    private Producer<String, GenericData.Record> producer;
    private Consumer<String, GenericData.Record> consumer;
    private final String topic;
    private final Properties props;

    public AvroExample(String brokers, String username, String password, String schemaRegistryUrl) throws IOException {
        this.topic = username + "-default2";

        String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
        String jaasCfg = String.format(jaasTemplate, username, password);

        props = new Properties();
        props.put("bootstrap.servers", brokers);
        props.put("group.id", username + "-consumer");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("auto.offset.reset", "earliest");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", KafkaAvroDeserializer.class.getName());
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", KafkaAvroSerializer.class.getName());
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "SCRAM-SHA-256");
        props.put("sasl.jaas.config", jaasCfg);
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("debug", "all");

        producer = new KafkaProducer<>(props);
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topic));
    }

    public void consume() {
        ConsumerRecords<String, GenericData.Record> records = consumer.poll(100);
        for (ConsumerRecord<String, GenericData.Record> record : records) {
            System.out.printf("%s [%d] offset=%d, key=%s, value=\"%s\"\n",
                    record.topic(), record.partition(),
                    record.offset(), record.key(), record.value());
        }

    }

    public void produce(GenericData.Record record) {
        System.out.println("Sending record to kafka");
        producer.send(new ProducerRecord<>(topic, record));
    }

    // class variable
    static final String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890".toLowerCase();
    static final java.util.Random rand = new java.util.Random();

    private static String randomString() {
        StringBuilder builder = new StringBuilder();
        int length = rand.nextInt(5) + 5;
        for (int i = 0; i < length; i++) {
            builder.append(lexicon.charAt(rand.nextInt(lexicon.length())));
        }
        return builder.toString();
    }

    private static GenericData.Record generateRecord(Schema schema, int index) {
        GenericRecordBuilder customerBuilder = new GenericRecordBuilder(schema);

        customerBuilder.set("first_name", randomString());
        customerBuilder.set("last_name", randomString());
        customerBuilder.set("age", rand.nextInt(index % 40 + 1) + 5);
        customerBuilder.set("city", "Stockholm");
        customerBuilder.set("height", 175f);
        customerBuilder.set("weight", 70.5f);
        customerBuilder.set("automated_email", index % 2 == 0);
        return customerBuilder.build();
    }

    public static void main(String[] args) throws IOException {
        final String brokers = System.getenv("CLOUDKARAFKA_BROKERS");
        final String username = System.getenv("CLOUDKARAFKA_USERNAME");
        final String password = System.getenv("CLOUDKARAFKA_PASSWORD");
        final String schemaUrl = System.getenv("CLOUDKARAFKA_SCHEMAREGISTRY");

        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(new File("avro/customer.avsc"));

        final AvroExample example = new AvroExample(brokers, username, password, schemaUrl);
        Thread t = new Thread(() -> {
            int i = 0;
            GenericData.Record record;
            while (true) {
                record = generateRecord(schema, i);
                example.produce(record);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
                i++;
            }
        });
        t.start();
        while (true) {
            example.consume();

        }
    }
}
