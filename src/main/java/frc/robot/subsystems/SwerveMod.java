package frc.robot.subsystems;

import static frc.robot.Conversions.modulate360;
import static frc.robot.Conversions.modulate4096;
import static frc.robot.Conversions.toCounts;
import static frc.robot.Conversions.toDegrees;
import static frc.robot.RobotMap.ANGLECONTINUOUSCURRENTLIMIT;
import static frc.robot.RobotMap.ANGLED;
import static frc.robot.RobotMap.ANGLEENABLECURRENTLIMIT;
import static frc.robot.RobotMap.ANGLEI;
import static frc.robot.RobotMap.ANGLEP;
import static frc.robot.RobotMap.ANGLEPEAKCURRENT;
import static frc.robot.RobotMap.ANGLEPEAKCURRENTDURATION;
import static frc.robot.RobotMap.DRIVECONTINUOUSCURRENTLIMIT;
import static frc.robot.RobotMap.DRIVEENABLECURRENTLIMIT;
import static frc.robot.RobotMap.DRIVEPEAKCURRENT;
import static frc.robot.RobotMap.DRIVEPEAKCURRENTDURATION;
import static frc.robot.RobotMap.SLOTIDX;
import static frc.robot.RobotMap.SWERVETIMEOUT;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.misc.math.Vector2;


public class SwerveMod{
    private double lastTargetAngle = 0;
    public final int moduleNumber;

    public final int mZeroOffset;

    private final TalonSRX mAngleMotor;
    private final TalonSRX mDriveMotor;
    private Vector2 modulePosition;
    private boolean driveInverted = false;
    
    public double targetAngle;
    public double targetSpeed;
    public double smartAngle;
    public Vector2 velocity;
    public double currentAngle;
    private int absolutePosition;
    private boolean invertDrive;

    public SwerveMod(int moduleNumber, Vector2 modulePosition, TalonSRX angleMotor, TalonSRX driveMotor, boolean invertDrive, boolean invertSensorPhase, int zeroOffset) {
        this.moduleNumber = moduleNumber;
        this.modulePosition = modulePosition;
        mAngleMotor = angleMotor;
        mDriveMotor = driveMotor;
        mZeroOffset = zeroOffset;
        targetAngle = 0;
        targetSpeed = 0;
        currentAngle = 0;
        this.invertDrive = invertDrive;


        // Configure Angle Motor
        angleMotor.configFactoryDefault();
        angleMotor.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, SLOTIDX, SWERVETIMEOUT);
        angleMotor.configFeedbackNotContinuous(false, SWERVETIMEOUT); //make false if using relative, make true if using absolute
        angleMotor.selectProfileSlot(SLOTIDX, SWERVETIMEOUT);
        angleMotor.setSensorPhase(invertSensorPhase);
        angleMotor.config_kP(SLOTIDX, ANGLEP, SWERVETIMEOUT);
        angleMotor.config_kI(SLOTIDX, ANGLEI, SWERVETIMEOUT);
        angleMotor.config_kD(SLOTIDX, ANGLED, SWERVETIMEOUT);
        angleMotor.setNeutralMode(NeutralMode.Brake);

        
        //Configre Drive Motor
        /*driveMotor.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, SLOTIDX, SWERVETIMEOUT);
        driveMotor.setSelectedSensorPosition(0, SLOTIDX, SWERVETIMEOUT);
        driveMotor.selectProfileSlot(SLOTIDX, SWERVETIMEOUT);
        driveMotor.config_kP(SLOTIDX, ANGLEP, SWERVETIMEOUT);
        driveMotor.config_kI(SLOTIDX, ANGLEI, SWERVETIMEOUT);
        driveMotor.config_kD(SLOTIDX, ANGLED, SWERVETIMEOUT);*/
        driveMotor.setNeutralMode(NeutralMode.Brake);

        
        // Setup Current Limiting
        angleMotor.configContinuousCurrentLimit(ANGLECONTINUOUSCURRENTLIMIT, SWERVETIMEOUT);
        angleMotor.configPeakCurrentLimit(ANGLEPEAKCURRENT, SWERVETIMEOUT);
        angleMotor.configPeakCurrentDuration(ANGLEPEAKCURRENTDURATION, SWERVETIMEOUT);
        angleMotor.enableCurrentLimit(ANGLEENABLECURRENTLIMIT);

        driveMotor.configContinuousCurrentLimit(DRIVECONTINUOUSCURRENTLIMIT, SWERVETIMEOUT);
        driveMotor.configPeakCurrentLimit(DRIVEPEAKCURRENT, SWERVETIMEOUT);
        driveMotor.configPeakCurrentDuration(DRIVEPEAKCURRENTDURATION, SWERVETIMEOUT);
        driveMotor.enableCurrentLimit(DRIVEENABLECURRENTLIMIT);

        

        
        setDriveInverted(invertDrive);
  

        absolutePosition = getTicks();
        if(invertSensorPhase){absolutePosition *= -1;}

        zero();
    }

    public TalonSRX getAngleMotor(){
        return mAngleMotor;
    }
    public double getCurrentAngle(){
        double angle = currentAngle;
        return angle;
    }


    public void setTargetVelocity(Vector2 velocity, boolean speed, double rotation){
            this.velocity = velocity;
            if(moduleNumber == 1){
                targetAngle = -(velocity.getAngle().toDegrees());
            }
            else{
                targetAngle = velocity.getAngle().toDegrees();
            }
            //targetAngle += (rotationDeadband(rotation) * 90);

            smartAngle = targetAngle;
            if(speed){
            targetSpeed = velocity.length;

            }
            else{
            targetSpeed = 0;
            }
    }
    public double getVelocityX(){
        return velocity.x;
    }
    public double getVelocityY(){
        return velocity.y;
    }

    public Vector2 getModulePosition(){
        return modulePosition;
    }

    public TalonSRX getDriveMotor() {
        return mDriveMotor;
    }

    public double getDrivePos(){
        return mDriveMotor.getSelectedSensorPosition(SLOTIDX);
    }
    
    public double getTargetAngle() {
        return lastTargetAngle;
    }

    public void setDriveInverted(boolean inverted) {
        driveInverted = inverted;
    }
    public void setTargetAngleVal(double targetAngle){
        this.targetAngle = targetAngle;
    }
    public void setTargetSpeedVal(double targetSpeed){
        this.targetSpeed = targetSpeed;
    }
    public void setTargetAngle(double targetAngle) {
        targetAngle = modulate360(targetAngle);
        double currentAngle = toDegrees(getPos());
        double currentAngleMod = modulate360(currentAngle);
        if (currentAngleMod < 0) currentAngleMod += 360;
        double delta = currentAngleMod - targetAngle;
        if (delta > 180) {
            targetAngle += 360;
        } else if (delta < -180) {
            targetAngle -= 360;
        }
        delta = currentAngleMod - targetAngle;
        if (delta > 90 || delta < -90) {
            if (delta > 90)
                targetAngle += 180;
            else if (delta < -90)
                targetAngle -= 180;
            mDriveMotor.setInverted(false);
        } else {
            mDriveMotor.setInverted(true);
        }

        targetAngle += currentAngle - currentAngleMod;
        //double currentError = getRawError();
        lastTargetAngle = targetAngle;
        
        targetAngle = toCounts(targetAngle);
        //smartAngle = targetAngle;
        mAngleMotor.set(ControlMode.Position, targetAngle);
    }

    public void setTargetSpeed(double speed) {
        if (driveInverted) {speed = -speed;}
        SmartDashboard.putNumber("speed "+ moduleNumber + "  ", speed);
        mDriveMotor.set(ControlMode.PercentOutput, speed);
    }

    public double getRawError(){
        return mAngleMotor.getClosedLoopError(0);
    }
    
    public double getAdjustedError(){
        return toDegrees(getRawError());
    }

    public  double getPos(){
        double relativePos = mAngleMotor.getSelectedSensorPosition(SLOTIDX);
        return relativePos;
    }

    /**
     * @return Ticks of Position
     */
    public int getTicks(){
        return mAngleMotor.getSensorCollection().getPulseWidthPosition() & 0xFFF;
    }
    public double getOffset(){
        return mZeroOffset;
    }
    public void zero(){
        mAngleMotor.setSelectedSensorPosition(modulate4096(absolutePosition + mZeroOffset), SLOTIDX, SWERVETIMEOUT);
    }
    //(int)( toCounts(modulate360( toDegrees(getPos()) ) ))

    public void setSensorPhase(boolean invert){
        mAngleMotor.setSensorPhase(invert);
    }

}
