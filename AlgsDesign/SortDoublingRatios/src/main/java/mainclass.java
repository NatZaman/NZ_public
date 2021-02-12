import java.util.Arrays;

import edu.yu.introtoalgs.SortImplementations;
import edu.yu.introtoalgs.Sorter;
import org.apache.commons.lang3.time.StopWatch;
import java.util.Random;

import static edu.yu.introtoalgs.SortImplementations.SortFactory;

public class mainclass {

    public static void main(String[] args){

        int n = 100;
        int tests = 10;
        int timesToDouble = 5;
        long prevLast = 1;
        long prevAvg = 1;

        Comparable[] a = new Comparable[n];
        a = fill(a);

        StopWatch watch = new StopWatch();
        long[][] times = new long[timesToDouble][tests];
        System.out.println(a.toString());

         for(int i = 0; i < timesToDouble; i++) {
             for (int j = 0; j < tests; j++) {
                 Comparable a1[] = new Comparable[a.length];
                 System.arraycopy(a, 0, a1, 0, a1.length);
                 watch.start();
                 Sorter sorter = SortFactory(SortImplementations.JDKSort );
                 sorter.sortIt(a1);
                 watch.stop();
                 times[i][j] = watch.getTime();
                 watch.reset();
             }
             double ratio = ((double)times[i][9]/prevLast);
             double  avgRatio = ((double)avgOfTimes(times, i)/prevAvg);
             System.out.printf("Doubled by %d, Size %d, First run(Warmup1/5): %d%n Last run time: %d, Avg runtime: %d%n ratio: %f, avg ratio: %f%n"
                             + " Predict next last: %d, Predict next avg: %d%n%n%n",
                     i,  a.length, times[i][0], (times[i][9]), avgOfTimes(times, i), ratio, avgRatio, times[i][9]*4, (avgOfTimes(times, i)*4));
             n = 2*n;
             a = new Comparable[n];
             a = fill(a);
             prevLast = times[i][9];
             prevAvg = (avgOfTimes(times, i));
         }
         System.out.printf("First: %d, %d%nSecond: %d, %d%nThird: %d, %d%nFourth: %d, %d%nFifth: %d, %d%n",
                 times[0][0], times[0][9],
                 times[1][0], times[1][9],
                 times[2][0], times[2][9],
                 times[3][0], times[3][9],
                 times[4][0], times[4][9]);
    }

    private static long avgOfTimes(long[][] times, int i){

        long avg = ((times[i][5])+ times[i][6]+ times[i][7]+ times[i][8]+ times[i][9])/5;
        return  avg;
    }


    private static Comparable[] doubleArray(Comparable a[]) {
        Random rng = new Random();
        Comparable[] twiceA = new Comparable[a.length * 2];
        for (int i = 0; i < twiceA.length-1; i++) {
            a[i] = rng.nextInt();
        }
        return twiceA;
    }

    private static Comparable[] fill(Comparable[] a) {

        Random rng = new Random();
        for (int i = 0; i < a.length; i++) {
            a[i] = rng.nextInt();
        }
        return a;
    }



}

