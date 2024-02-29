package Team4450.Robot24.subsystems;

import static Team4450.Robot24.Constants.ELEVATOR_CENTERSTAGE_FACTOR;
import static Team4450.Robot24.Constants.ELEVATOR_MOTOR_INNER;
import static Team4450.Robot24.Constants.ELEVATOR_MOTOR_LEFT;
import static Team4450.Robot24.Constants.ELEVATOR_MOTOR_RIGHT;
import static Team4450.Robot24.Constants.ELEVATOR_WINCH_FACTOR;

import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkLimitSwitch;
import com.revrobotics.SparkPIDController;
import com.revrobotics.CANSparkBase.ControlType;
import com.revrobotics.CANSparkBase.IdleMode;
import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.SparkLimitSwitch.Type;

import Team4450.Lib.Util;
import Team4450.Robot24.AdvantageScope;
import Team4450.Robot24.Robot;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Elevator extends SubsystemBase {
    private CANSparkMax motorMain = new CANSparkMax(ELEVATOR_MOTOR_LEFT, MotorType.kBrushless);
    private CANSparkMax motorFollower = new CANSparkMax(ELEVATOR_MOTOR_RIGHT, MotorType.kBrushless);
    private CANSparkMax motorCenterstage = new CANSparkMax(ELEVATOR_MOTOR_INNER, MotorType.kBrushless);

    private PIDController mainPID;
    private PIDController followerPID;
    private PIDController centerstagePID;

    private SparkLimitSwitch lowerLimitSwitch;
    private SparkLimitSwitch upperLimitSwitch;
  
    private RelativeEncoder mainEncoder;
    private RelativeEncoder followEncoder;
    private RelativeEncoder centerstageEncoder;

    private final double MAIN_TOLERANCE = 0.7;
    private final double CENTERSTAGE_TOLERANCE = 1;
    

    public Elevator() {
        Util.consoleLog();

        // follower is mirrored and reversed
        // don't change this it's very important as shafts are linked with coupler
        // and will shatter if driven in opposite directions
        motorFollower.follow(motorMain, true);
        motorFollower.setInverted(true);

        motorFollower.setIdleMode(IdleMode.kBrake);
        motorMain.setIdleMode(IdleMode.kBrake);
        motorCenterstage.setIdleMode(IdleMode.kCoast);

        lowerLimitSwitch = motorFollower.getReverseLimitSwitch(Type.kNormallyOpen);
        upperLimitSwitch = motorFollower.getForwardLimitSwitch(Type.kNormallyOpen);

        lowerLimitSwitch.enableLimitSwitch(true);
        upperLimitSwitch.enableLimitSwitch(true);

        mainEncoder = motorMain.getEncoder();
        followEncoder = motorFollower.getEncoder();
        centerstageEncoder = motorCenterstage.getEncoder();

        // mainEncoder.setPositionConversionFactor(-1);
        // followEncoder.setPositionConversionFactor(-1);

        mainPID = new PIDController(0.06, 0, 0);
        SmartDashboard.putData("winch_pid", mainPID);
        mainPID.setTolerance(MAIN_TOLERANCE);
        // followerPID = new PIDController(0.01, 0, 0);
        centerstagePID = new PIDController(0.01, 0, 0);
        centerstagePID.setTolerance(CENTERSTAGE_TOLERANCE);
    }

    // private void configurePID(SparkPIDController pidController, double p, double i, double d) {
    //     pidController.setP(p);
    //     pidController.setI(i);
    //     pidController.setD(d);
    // }

    @Override
    public void periodic() {
        // if (lowerLimitSwitch.isPressed()) {
        //     mainEncoder.setPosition(0); // reset the encoder counts
        //     followEncoder.setPosition(0);
        //     centerstageEncoder.setPosition(0);[]
        // }
        SmartDashboard.putNumber("winch_measured", mainEncoder.getPosition());
        SmartDashboard.putNumber("centerstage_measured", centerstageEncoder.getPosition());
        
        AdvantageScope.getInstance().setElevatorHeight(getElevatorHeight());
        AdvantageScope.getInstance().setCarriageHeight(getCenterstageHeight());

        SmartDashboard.putNumber("winch_1_m", mainEncoder.getPosition() * ELEVATOR_WINCH_FACTOR);
        SmartDashboard.putNumber("windh_2_m", followEncoder.getPosition() * ELEVATOR_WINCH_FACTOR);
        SmartDashboard.putNumber("centerstage_m", centerstageEncoder.getPosition() * ELEVATOR_CENTERSTAGE_FACTOR);        
    }

    /**
     * move elevator in direction based on speed
     * @param speed (such as from a joystick value)
     */
    public void move(double speed) {
        if (speed < 0)
            speed *= 0.1;
        speed *= -0.5;
        motorMain.set(speed);
        if (Robot.isSimulation()) {
            if (speed > 0) speed *= 10;
            mainEncoder.setPosition(mainEncoder.getPosition() + (1*speed));
            followEncoder.setPosition(followEncoder.getPosition() + (1*speed));
        }
    }

    public void moveCenterStage(double speed) {
        motorCenterstage.set(speed);
        if (Robot.isSimulation()) {
            centerstageEncoder.setPosition(centerstageEncoder.getPosition() + (1*speed));
        }
    }

    public void setElevatorHeight(double height) {
        double setpoint = height / ELEVATOR_WINCH_FACTOR; // meters -> enc. counts
        SmartDashboard.putNumber("winch_setpoint", setpoint);

        mainPID.setSetpoint(setpoint);
        double nonclamped = mainPID.calculate(mainEncoder.getPosition());
            SmartDashboard.putNumber("winch_nonclamped", nonclamped);
        double motorOutput = nonclamped;//Util.clampValue(nonclamped, 0.2);
                SmartDashboard.putNumber("winch_output", motorOutput);

        motorMain.set(motorOutput);

        if (Robot.isSimulation()) mainEncoder.setPosition(mainEncoder.getPosition() + (1*motorOutput));
        if (Robot.isSimulation()) followEncoder.setPosition(followEncoder.getPosition() + (1*motorOutput));
    }

    public void setCenterstageHeight(double height) {
        double setpoint = height / ELEVATOR_CENTERSTAGE_FACTOR; // meters -> enc. counts
        SmartDashboard.putNumber("centerstage_setpoint", setpoint);

        centerstagePID.setSetpoint(setpoint);
        double motorOutput = Util.clampValue(centerstagePID.calculate(centerstageEncoder.getPosition()), 0.2);
        motorCenterstage.set(motorOutput);

        if (Robot.isSimulation()) centerstageEncoder.setPosition(centerstageEncoder.getPosition() + (1*motorOutput));
    }

    public boolean elevatorIsAtHeight(double height) {
        double setpoint = height / ELEVATOR_WINCH_FACTOR;
        return Math.abs(setpoint - mainEncoder.getPosition()) < MAIN_TOLERANCE;
    }

    public boolean centerstageIsAtHeight(double height) {
        double setpoint = height / ELEVATOR_CENTERSTAGE_FACTOR;
        return Math.abs(setpoint - centerstageEncoder.getPosition()) < CENTERSTAGE_TOLERANCE;
    }

    public double getElevatorHeight() {
        double mainValue = mainEncoder.getPosition() * ELEVATOR_WINCH_FACTOR;
        return mainValue;
        // double followValue = followEncoder.getPosition() * ELEVATOR_WINCH_FACTOR;
        // return (0.5 * (mainValue + followValue)); // mean
    }

    public double getCenterstageHeight() {return centerstageEncoder.getPosition() * ELEVATOR_CENTERSTAGE_FACTOR;}

    public void resetEncoders() {
        mainEncoder.setPosition(-10.14);
        followEncoder.setPosition(-10.14);
        centerstageEncoder.setPosition(0);
    }

    // /**
    //  * The height of the elevator (measured at shooter pivot)
    //  * above the ground
    //  * @return the height in meters of MAXSpline shaft above ground
    //  */
    // public double getHeight() {
    //     double avgCounts = 0.5 * (mainEncoder.getPosition() + followEncoder.getPosition());
    //     return avgCounts;
    // }

}
