package timeseries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HoltWinters {
	List<Double> trainingData;
	Double alpha;
	Double beta;
	Double gamma;
	Integer period;
	Integer m;

	Integer seasons;
	Double initialLevel;
	Double initialTrend;
	Map<Integer, Double> initialSeasonalIndices = new TreeMap<Integer, Double>();

	public HoltWinters(List<Double> trainingData) {
		super();
		this.trainingData = trainingData;
		this.alpha = 0.7d;
		this.beta = 0.03d;
		this.gamma = 0.9d;
		this.period = 96;
		this.m = 36;
		computeSeasons();
		computeInitialLevel();
		computeInitialTrend();
		computeSeasonalIndices();
		printObservations();
	}

	public void printObservations() {
		System.out.println("Seasons: " + seasons);
		System.out.println("Periods: " + period);
		System.out.println("Initial a0 (level): " + initialLevel);
		System.out.println("Initial b0 (trend): " + initialTrend);
		System.out.println("Initial seasonal indices : " + initialSeasonalIndices);
	}

	public Map<Integer, Double> forecast() {
		Map<Integer, Double> forecastMap = new TreeMap<Integer, Double>();
		Double smooth = 0d;
		Double trend = 0d;
		for (int i = 0; i < trainingData.size() + m; i++) {
			if (i == 0) {
				smooth = trainingData.get(0);
				trend = initialTrend;
				forecastMap.put(i, trainingData.get(0));
				continue;
			}
			if (i >= trainingData.size()) {
				Integer mm = i - trainingData.size() + 1;
				forecastMap.put(i, (smooth + mm * trend) + initialSeasonalIndices.get(i % period));
			} else {
				Double val = trainingData.get(i);
				Double lastSmooth = smooth;
				smooth = alpha * (val - initialSeasonalIndices.get(i % period)) + (1d - alpha) * (smooth + trend);
				trend = beta * (smooth - lastSmooth) + (1d - beta) * trend;
				initialSeasonalIndices.put(i % period,
						gamma * (val - smooth) + (1 - gamma) * initialSeasonalIndices.get(i % period));
				forecastMap.put(i, smooth + trend + initialSeasonalIndices.get(i % period));
			}
		}
		return forecastMap;
	}

	private void computeSeasons() {
		this.seasons = trainingData.size() / period;
	}

	private void computeInitialLevel() {
		this.initialLevel = trainingData.get(0);
	}

	private void computeInitialTrend() {
		Double sum = 0d;
		for (int i = 0; i < period; i++) {
			sum += (trainingData.get(period + i) - trainingData.get(i)) / period;
		}
		this.initialTrend = sum / period;
	}

	private void computeSeasonalIndices() {
		List<Double> seasonalAverage = new ArrayList<Double>();
		Map<Integer, Double> seasonalIndices = new TreeMap<Integer, Double>();

		// season averages
		for (int j = 0; j < seasons; j++) {
			Double sum = 0d;
			for (int i = period * j; i <= (period * j + period); i++) {
				sum += trainingData.get(i);
			}
			seasonalAverage.add(sum / period);
		}

		// initial values
		for (int i = 0; i < period; i++) {
			Double sumOfValuesOverAvg = 0d;
			for (int j = 0; j < seasons; j++) {
				sumOfValuesOverAvg += trainingData.get(period * j + i) - seasonalAverage.get(j);
			}
			seasonalIndices.put(i, sumOfValuesOverAvg / seasons);
		}
		this.initialSeasonalIndices = seasonalIndices;
	}
}
