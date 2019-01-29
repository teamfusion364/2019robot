package frc.robot.commands.teleop.defaultcommands;

import edu.wpi.first.wpilibj.command.Command;
import frc.robot.Robot;

public class TeleopArmCommand extends Command {

    /**
     * Command used for teleop control specific to the arn System
     * <p>Operator controled manually
     */
    public TeleopArmCommand() {
        requires(Robot.liftSystem);
        //Other commands can interrupt this command
        setInterruptible(true);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void execute() {
    }

    @Override
    protected void interrupted() {
        end();
    }
    @Override
    protected void end() {
        Robot.driveSystem.stop();
    }

    @Override
    protected boolean isFinished() {
        // This command will only end when interrupted during teleop mode
        //by buttons in the Operator Interface
        return false;
    }

}
