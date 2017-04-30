package timeseries;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class TimeSeriesPrediction {

	public static void main(String[] args) {
		MongoClient mongo;
		try {
			mongo = new MongoClient("localhost", 27017);
			DB db = mongo.getDB("timeseries");

			// aggregate raw sensor data and write results in a new collection
			DBCollection rawSensorData = db.getCollection("sensordata");
			DBCollection temperatureData = db.getCollection("temperature");
			DBCollection plot = db.getCollection("plot");

			temperatureData.drop();
			plot.drop();

			AggregationOutput output = rawSensorData
					.aggregate(new BasicDBObject("$unwind", "$record.sdata"),
							new BasicDBObject("$project", new BasicDBObject("sensors", "$record.sdata.sensors")
									.append("timestamp", "$record.sdata.timestamp")),
							new BasicDBObject("$unwind", "$sensors"),
							new BasicDBObject("$match", new BasicDBObject("sensors.stype", "Ext_Tem")),
							new BasicDBObject("$project", new BasicDBObject("_id", 0).append("timestamp", "$timestamp")
									.append("sensorValue", "$sensors.value")),
							new BasicDBObject("$out", "temperature"));
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Map<Date, Double> trainingData = new TreeMap<Date, Double>();

			double limit = temperatureData.count() / 3;
			DBCursor temperatureCursor = temperatureData.find().limit(Double.valueOf(limit).intValue());

			while (temperatureCursor.hasNext()) {
				DBObject temperature = temperatureCursor.next();
				trainingData.put(simpleDateFormat.parse(temperature.get("timestamp").toString()),
						((Number) temperature.get("sensorValue")).doubleValue());

			}

			Integer index = 0;
			DBCursor allSensorData = temperatureData.find();
			while (allSensorData.hasNext()) {
				DBObject temperature = allSensorData.next();
				plot.update(new BasicDBObject("index", index),
						new BasicDBObject("$set",
								new BasicDBObject("measured", ((Number) temperature.get("sensorValue")).doubleValue())
										.append("index", index)),
						true, false);
				index++;
			}

			System.out.println(trainingData);
			ArrayList<Double> values = new ArrayList<Double>();
			for (Double value : trainingData.values()) {
				values.add(value);
			}

			HoltWinters holtWinters = new HoltWinters(values);
			Map<Integer, Double> forecastedValues = holtWinters.forecast();
			// System.out.println(forecastedValues);

			for (Map.Entry<Integer, Double> forecasted : forecastedValues.entrySet()) {
				plot.update(new BasicDBObject("index", forecasted.getKey() + 1), new BasicDBObject("$set",
						new BasicDBObject("forecasted", forecasted.getValue()).append("index", forecasted.getKey())),
						true, false);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
