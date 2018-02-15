package timely.adapter.accumulo;

import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.client.lexicoder.PairLexicoder;
import org.apache.accumulo.core.client.lexicoder.StringLexicoder;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.util.ComparablePair;
import org.apache.accumulo.core.util.Pair;
import timely.model.Metric;
import timely.model.Tag;
import timely.model.parse.TagListParser;
import timely.model.parse.TagParser;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Creates Accumulo data structures from {@link timely.model.Metric}
 */
public class MetricAdapter {

    private static final PairLexicoder<String, Long> rowCoder = new PairLexicoder<>(new StringLexicoder(),
            new LongLexicoder());

    private static final TagParser tagParser = new TagParser();
    private static final TagListParser tagListParser = new TagListParser();

    public static final ColumnVisibility EMPTY_VISIBILITY = new ColumnVisibility();
    public static final String VISIBILITY_TAG = "viz";

    public static long roundTimestampToLastHour(long timestamp) {
        return timestamp - (timestamp % 3600000);
    }

    public static long roundTimestampToNextHour(long timestamp) {
        return timestamp - (timestamp % 3600000) + 3600000;
    }

    public static Mutation toMutation(Metric metric) {
        final Mutation mutation = new Mutation(encodeRowKey(metric));

        List<Tag> tags = metric.getTags();
        Collections.sort(tags);

        for (final Tag entry : tags) {
            if (entry.getKey().equals(VISIBILITY_TAG))
                continue;

            final String cf = entry.join();
            // @formatter:off
            String timestamp = Long.toString(metric.getValue().getTimestamp());
            String cq = timestamp + "\0" + tags.stream().filter(inner -> !inner.equals(entry))
                    .filter(inner -> !inner.getKey().equals(VISIBILITY_TAG))
                    .map(Tag::join)
                    .collect(Collectors.joining(","));
            // @formatter:on

            mutation.put(cf, cq, extractVisibility(tags), metric.getValue().getTimestamp(), extractValue(metric));
        }
        return mutation;
    }

    private static Value extractValue(Metric metric) {
        return new Value(encodeValue(metric.getValue().getMeasure()));
    }

    public static byte[] encodeValue(Double d) {
        byte[] b = new byte[Double.BYTES];
        ByteBuffer bb = ByteBuffer.wrap(b);
        bb.putDouble(d);
        return b;
    }

    public static Double decodeValue(byte[] buf) {
        ByteBuffer bb = ByteBuffer.wrap(buf);
        return bb.getDouble();
    }

    private static ColumnVisibility extractVisibility(List<Tag> tags) {
        // @formatter:off
        Optional<Tag> visTag = tags.stream()
                .filter(t -> t.getKey().equals(VISIBILITY_TAG))
                .findFirst();
        return visTag.isPresent() ? new ColumnVisibility(visTag.get().getValue()) : EMPTY_VISIBILITY;
        // @formatter:on
    }

    public static Metric parse(Key k, Value v, boolean includeVizTag) {
        ComparablePair<String, Long> row = rowCoder.decode(k.getRow().getBytes());
        // @formatter:off
        Metric.Builder builder = Metric.newBuilder()
                .name(row.getFirst())
                .value(k.getTimestamp(), ByteBuffer.wrap(v.get()).getDouble())
                .tag(tagParser.parse(k.getColumnFamily().toString()));
        // @formatter:on
        String cf = k.getColumnQualifier().toString();
        int x = cf.indexOf("\0");
        if (x >= 0) {
            cf = cf.substring(x + 1);
        }
        tagListParser.parse(cf).forEach(builder::tag);
        if (includeVizTag && k.getColumnVisibility().getLength() > 0) {
            tagListParser.parse("viz=" + k.getColumnVisibility().toString()).forEach(builder::tag);
        }
        return builder.build();
    }

    public static Metric parse(Key k, Value v) {
        return parse(k, v, false);
    }

    public static byte[] encodeRowKey(String metricName, Long timestamp) {
        return rowCoder.encode(new ComparablePair<>(metricName, timestamp));
    }

    public static byte[] encodeRowKey(Metric metric) {
        // round timestamp to hour for scan efficiency and compression
        return encodeRowKey(metric.getName(), roundTimestampToLastHour(metric.getValue().getTimestamp()));
    }

    public static Pair<String, Long> decodeRowKey(Key k) {
        return rowCoder.decode(k.getRow().getBytes());
    }
}