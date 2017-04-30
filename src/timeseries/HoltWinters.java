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
		this.alpha = 0.5d;
		this.beta = 0.2d;
		this.gamma = 0.415d;
		this.period = 96;
		this.m = 2700;
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
		Map<Integer, Double> smooth = new TreeMap<Integer, Double>();
		Map<Integer, Double> trend = new TreeMap<Integer, Double>();
		Double finalSmooth = 0d;
		Double finalTrend = 0d;
		for (int i = 0; i < trainingData.size() + m; i++) {
			if (i == 0) {
				smooth.put(0, trainingData.get(0));
				trend.put(0, initialTrend);
				forecastMap.put(i, trainingData.get(0));
				continue;
			}

			if (i > trainingData.size() - 1) { // forecast
				// smooth.put(i, alpha * (val - initialSeasonalIndices.get(i %
				// period))
				// + (1d - alpha) * (smooth.get(i - 1) + trend.get(i - 1)));
				// trend.put(i, beta * (smooth.get(i) - smooth.get(i - 1)) + (1d
				// - beta) * trend.get(i - 1));
				// if (i > trainingData.size() + 1) {
				Integer mm = i - trainingData.size() + 1;
				// initialSeasonalIndices.put(i % period, gamma * (val /
				// smooth.get(i % period))
				// + (1 - gamma) * initialSeasonalIndices.get(i % period));
				forecastMap.put(i, (finalSmooth + mm * finalTrend) + initialSeasonalIndices.get(i % period));
				// }
			} else {
				Double val = trainingData.get(i);
				smooth.put(i, alpha * (val - initialSeasonalIndices.get(i % period))
						+ (1d - alpha) * (smooth.get(i - 1) + trend.get(i - 1)));
				trend.put(i, beta * (smooth.get(i) - smooth.get(i - 1)) + (1d - beta) * trend.get(i - 1));
				initialSeasonalIndices.put(i % period,
						gamma * (val - smooth.get(i)) + (1 - gamma) * initialSeasonalIndices.get(i % period));
				forecastMap.put(i, (smooth.get(i) + trend.get(i) + initialSeasonalIndices.get(i % period)));
				finalSmooth = smooth.get(i);
				finalTrend = trend.get(i);
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
		this.initialTrend = sum / (period);
	}

	private void computeSeasonalIndices() {
		List<Double> seasonalAverage = new ArrayList<Double>();
		List<Double> averageObservations = new ArrayList<Double>();
		Map<Integer, Double> seasonalIndices = new TreeMap<Integer, Double>();

		// season averages
		for (int j = 0; j < seasons; j++) {
			Double sum = 0d;
			for (int i = 0; i < period; i++) {
				sum += trainingData.get(j * period + i);
			}
			seasonalAverage.add(sum / period);
		}

		for (int i = 0; i < seasons; i++) {
			for (int j = 0; j < period; j++) {
				averageObservations.add(i * period + j, trainingData.get(i * period + j) / seasonalAverage.get(i));
			}
		}

		for (int i = 0; i < period; i++) {
			double sum = 0;
			for (int j = 0; j < seasons; j++) {
				sum += averageObservations.get(j * period + i);
			}
			seasonalIndices.put(i, sum / seasons);
		}

		// // initial values
		// for (int i = 0; i < period; i++) {
		// Double sumOfValuesOverAvg = 0d;
		// for (int j = 0; j < seasons; j++) {
		// sumOfValuesOverAvg += trainingData.get(period * j + i) -
		// seasonalAverage.get(j);
		// }
		// seasonalIndices.put(i, sumOfValuesOverAvg / seasons);
		// }
		this.initialSeasonalIndices = seasonalIndices;
	}
}
