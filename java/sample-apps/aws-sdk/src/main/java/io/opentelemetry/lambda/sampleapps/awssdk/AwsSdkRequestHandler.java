package io.opentelemetry.lambda.sampleapps.awssdk;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

public class AwsSdkRequestHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  // Declare metric variables
  public static long queueSizeChange;
  public static long apiBytesSent;
  public static long totalBytesSent;

  private static final Logger logger = LogManager.getLogger(AwsSdkRequestHandler.class);
  private static final Meter meter =
      GlobalOpenTelemetry.getMeterProvider()
          .meterBuilder("adot")
          .setInstrumentationVersion("1.0")
          .build();

  private static final AttributeKey<String> API_NAME = AttributeKey.stringKey("apiName");
  private static final AttributeKey<String> STATUS_CODE = AttributeKey.stringKey("statuscode");
  private static final Attributes METRIC_ATTRIBUTES =
      Attributes.builder().put(API_NAME, "apiName").put(STATUS_CODE, "200").build();

  private static final ObservableLongUpDownCounter queueSizeCounter =
      meter
          .upDownCounterBuilder("queueSizeChange")
          .setDescription("Queue Size change")
          .setUnit("one")
          .buildWithCallback(measurement -> measurement.record(queueSizeChange, METRIC_ATTRIBUTES));

  // building synchronous request-based counter metric
  private static final ObservableLongCounter totalApiBytesSentMetric =
      meter
          .counterBuilder("totalApiBytesSentMetricName")
          .setDescription("API request load sent in bytes")
          .setUnit("1")
          .buildWithCallback(measurement -> measurement.record(totalBytesSent, METRIC_ATTRIBUTES));

  private static final ObservableLongGauge apiBytesSentMetric =
      meter
          .gaugeBuilder("apiBytesSentMetricName")
          .setDescription("Testing Guage")
          .setUnit("ms")
          .ofLongs()
          .buildWithCallback(
              measurement -> {
                measurement.record(apiBytesSent, METRIC_ATTRIBUTES);
              });

  // building histogram request-based metric
  private static final DoubleHistogram latencyMetric =
      meter
          .histogramBuilder("latencyMetricName")
          .setDescription("API latency time")
          .setUnit("ms")
          .build();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(
      APIGatewayProxyRequestEvent input, Context context) {
    logger.info("Serving lambda request.");

    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    try (S3Client s3 = S3Client.create()) {
      ListBucketsResponse listBucketsResponse = s3.listBuckets();
      response.setBody(
          "Hello lambda - found " + listBucketsResponse.buckets().size() + " buckets.");
    }

    // Generate sample metrics using OTel-Java
    queueSizeChange = ThreadLocalRandom.current().nextLong(100);
    apiBytesSent = input.toString().length() + ThreadLocalRandom.current().nextLong(100);
    totalBytesSent += apiBytesSent;
    latencyMetric.record(System.currentTimeMillis(), METRIC_ATTRIBUTES);
    return response;
  }
}
