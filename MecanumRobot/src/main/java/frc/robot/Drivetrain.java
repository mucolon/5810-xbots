// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.AnalogGyro;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.PWMVictorSPX;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.controller.PIDController;
import edu.wpi.first.wpilibj.controller.SimpleMotorFeedforward;
import edu.wpi.first.wpilibj.geometry.Translation2d;
import edu.wpi.first.wpilibj.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.kinematics.MecanumDriveKinematics;
import edu.wpi.first.wpilibj.kinematics.MecanumDriveOdometry;
import edu.wpi.first.wpilibj.kinematics.MecanumDriveWheelSpeeds;

/** Represents a mecanum drive style drivetrain. */
@SuppressWarnings("PMD.TooManyFields")
public class Drivetrain {
  public static final double kMaxSpeed = 3.0; // 3 meters per second
  public static final double kMaxAngularSpeed = Math.PI; // 1/2 rotation per second

  private final SpeedController m_frontLeftMotor = new PWMVictorSPX(1);
  private final SpeedController m_frontRightMotor = new PWMVictorSPX(2);
  private final SpeedController m_backLeftMotor = new PWMVictorSPX(3);
  private final SpeedController m_backRightMotor = new PWMVictorSPX(4);

  private final Encoder m_frontLeftEncoder = new Encoder(0, 1);
  private final Encoder m_frontRightEncoder = new Encoder(2, 3);
  private final Encoder m_backLeftEncoder = new Encoder(4, 5);
  private final Encoder m_backRightEncoder = new Encoder(6, 7);

  private final Translation2d m_frontLeftLocation = new Translation2d(0.381, 0.381);
  private final Translation2d m_frontRightLocation = new Translation2d(0.381, -0.381);
  private final Translation2d m_backLeftLocation = new Translation2d(-0.381, 0.381);
  private final Translation2d m_backRightLocation = new Translation2d(-0.381, -0.381);

  private final PIDController m_frontLeftPIDController = new PIDController(1, 0, 0);
  private final PIDController m_frontRightPIDController = new PIDController(1, 0, 0);
  private final PIDController m_backLeftPIDController = new PIDController(1, 0, 0);
  private final PIDController m_backRightPIDController = new PIDController(1, 0, 0);

  private final AnalogGyro m_gyro = new AnalogGyro(0);

  private final MecanumDriveKinematics m_kinematics =
      new MecanumDriveKinematics(
          m_frontLeftLocation, m_frontRightLocation, m_backLeftLocation, m_backRightLocation);

  private final MecanumDriveOdometry m_odometry =
      new MecanumDriveOdometry(m_kinematics, m_gyro.getRotation2d());

  // Gains are for example purposes only - must be determined for your own robot!
  private final SimpleMotorFeedforward m_feedforward = new SimpleMotorFeedforward(1, 3);

  /** Constructs a MecanumDrive and resets the gyro. */
  public Drivetrain() {
    m_gyro.reset();
  }

  /**
   * Returns the current state of the drivetrain.
   *
   * @return The current state of the drivetrain.
   */
  public MecanumDriveWheelSpeeds getCurrentState() {
    return new MecanumDriveWheelSpeeds(
        m_frontLeftEncoder.getRate(),
        m_frontRightEncoder.getRate(),
        m_backLeftEncoder.getRate(),
        m_backRightEncoder.getRate());
  }

  /**
   * Set the desired speeds for each wheel.
   *
   * @param speeds The desired wheel speeds.
   */
  public void setSpeeds(MecanumDriveWheelSpeeds speeds) {
    final double frontLeftFeedforward = m_feedforward.calculate(speeds.frontLeftMetersPerSecond);
    final double frontRightFeedforward = m_feedforward.calculate(speeds.frontRightMetersPerSecond);
    final double backLeftFeedforward = m_feedforward.calculate(speeds.rearLeftMetersPerSecond);
    final double backRightFeedforward = m_feedforward.calculate(speeds.rearRightMetersPerSecond);

    final double frontLeftOutput =
        m_frontLeftPIDController.calculate(
            m_frontLeftEncoder.getRate(), speeds.frontLeftMetersPerSecond);
    final double frontRightOutput =
        m_frontRightPIDController.calculate(
            m_frontRightEncoder.getRate(), speeds.frontRightMetersPerSecond);
    final double backLeftOutput =
        m_backLeftPIDController.calculate(
            m_backLeftEncoder.getRate(), speeds.rearLeftMetersPerSecond);
    final double backRightOutput =
        m_backRightPIDController.calculate(
            m_backRightEncoder.getRate(), speeds.rearRightMetersPerSecond);

    m_frontLeftMotor.setVoltage(frontLeftOutput + frontLeftFeedforward);
    m_frontRightMotor.setVoltage(frontRightOutput + frontRightFeedforward);
    m_backLeftMotor.setVoltage(backLeftOutput + backLeftFeedforward);
    m_backRightMotor.setVoltage(backRightOutput + backRightFeedforward);
  }

  /**
   * Method to drive the robot using joystick info.
   *
   * @param xSpeed Speed of the robot in the x direction (forward).
   * @param ySpeed Speed of the robot in the y direction (sideways).
   * @param rot Angular rate of the robot.
   * @param fieldRelative Whether the provided x and y speeds are relative to the field.
   */
  @SuppressWarnings("ParameterName")
  public void drive(double xSpeed, double ySpeed, double rot, boolean fieldRelative) {
    var mecanumDriveWheelSpeeds =
        m_kinematics.toWheelSpeeds(
            fieldRelative
                ? ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, m_gyro.getRotation2d())
                : new ChassisSpeeds(xSpeed, ySpeed, rot));
    mecanumDriveWheelSpeeds.normalize(kMaxSpeed);
    setSpeeds(mecanumDriveWheelSpeeds);
  }

  /** Updates the field relative position of the robot. */
  public void updateOdometry() {
    m_odometry.update(m_gyro.getRotation2d(), getCurrentState());
  }
}
