addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.8")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Yno-adapted-args"
)
