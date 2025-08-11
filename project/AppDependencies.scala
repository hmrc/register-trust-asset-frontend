import sbt.*

object AppDependencies {

  val bootstrapVersion = "9.19.0"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-30"            % "12.1.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping-play-30" % "3.3.0",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30"            % bootstrapVersion
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.scalatestplus" %% "scalacheck-1-18"        % "3.2.19.0",
    "wolfendale"        %% "scalacheck-gen-regexp"  % "0.1.2"
  ).map(_ % Test)

  def apply(): Seq[ModuleID]      = compile ++ test
}
