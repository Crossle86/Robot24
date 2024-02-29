package Team4450.Robot24.commands;

import Team4450.Lib.Util;
import Team4450.Robot24.subsystems.Intake;
import edu.wpi.first.wpilibj2.command.Command;

public class ReverseIntake extends Command {
    private final Intake  intake;

    public ReverseIntake(Intake intake) {
        this.intake = intake;
        addRequirements(intake);
    }

    @Override
    public void execute() {
        intake.start(-1);
    }

    @Override
    public void end(boolean interrupted) {
        Util.consoleLog("interrupted=%b", interrupted);
        intake.stop();
    }
}
