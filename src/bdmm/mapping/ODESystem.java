package bdmm.mapping;

import bdmm.parameterization.Parameterization;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.events.EventHandler;

public class ODESystem implements FirstOrderDifferentialEquations, EventHandler {

    private Parameterization param;
    private int interval;
    private double nextIntevalBoundaryTime;

    public ODESystem(Parameterization parameterization) {
        this.param = parameterization;
    }

    public void setInterval(int interval) {
        this.interval = interval;

        if (this.interval>0)
            this.nextIntevalBoundaryTime = param.getIntervalEndTimes()[interval-1];
    }


    /* FirstOrderDifferentialEquations implementation */

    @Override
    public int getDimension() {
        return param.getNTypes()*2;
    }

    @Override
    public void computeDerivatives(double t, double[] y, double[] yDot)
            throws MaxCountExceededException, DimensionMismatchException {

        int nTypes = param.getNTypes();

        for (int i = 0; i<nTypes; i++){

			/*  p0 equations (0 .. dim-1) */

			yDot[i] = + (param.getBirthRates()[interval][i]
                    + param.getDeathRates()[interval][i]
                    + param.getSamplingRates()[interval][i]) * y[i]
					- param.getBirthRates()[interval][i] * y[i] * y[i]
					- param.getDeathRates()[interval][i];

			for (int j = 0; j < nTypes; j++){

			    if (i==j)
			        continue;

                yDot[i] += param.getCrossBirthRates()[interval][i][j]*y[i];
                yDot[i] -= param.getCrossBirthRates()[interval][i][j]*y[i]*y[j];

                yDot[i] += param.getMigRates()[interval][i][j] * y[i];
                yDot[i] -= param.getMigRates()[interval][i][j] * y[j];
			}

			/*  log(ge) equations: (dim .. 2*dim-1) */

			yDot[nTypes + i] = + (param.getBirthRates()[interval][i]
                    + param.getDeathRates()[interval][i]
                    + param.getSamplingRates()[interval][i])
					- 2*param.getBirthRates()[interval][i]*y[i];

			for (int j = 0; j< nTypes; j++) {

                if (i==j)
			        continue;

                yDot[nTypes + i] += param.getCrossBirthRates()[interval][i][j];
                yDot[nTypes + i] -= param.getCrossBirthRates()[interval][i][j]
                        * (y[j] + y[i]*Math.exp(y[nTypes + j] - y[nTypes + i]));

                yDot[nTypes + i] += param.getMigRates()[interval][i][j];
                yDot[nTypes + i] -= param.getMigRates()[interval][i][j]
                        * Math.exp(y[nTypes + j]-y[nTypes+i]);
			}
		}

    }

    /* EventHandler Implementation */

    @Override
    public void init(double v, double[] doubles, double v1) { }

    @Override
    public double g(double v, double[] doubles) {
        return v - nextIntevalBoundaryTime;
    }

    @Override
    public Action eventOccurred(double t, double[] y, boolean increasing) {
        return Action.RESET_STATE;
    }

    @Override
    public void resetState(double t, double[] y) {

        this.interval -= 1;
    }
}