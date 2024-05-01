import sbt.*

object AppDependencies {

  val bootstrapVersion = "8.5.0"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-30"            % "8.5.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping-play-30" % "2.0.0",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30"            % bootstrapVersion
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-30"  % bootstrapVersion,
    "org.scalatest"       %% "scalatest"               % "3.2.18",
    "org.scalatestplus"   %% "scalacheck-1-17"         % "3.2.18.0",
    "org.jsoup"            % "jsoup"                   % "1.17.2",
    "org.mockito"         %% "mockito-scala-scalatest" % "1.17.31",
    "org.wiremock"         % "wiremock-standalone"     % "3.5.4",
    "wolfendale"          %% "scalacheck-gen-regexp"   % "0.1.2",
    "com.vladsch.flexmark" % "flexmark-all"              % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID]      = compile ++ test
}
