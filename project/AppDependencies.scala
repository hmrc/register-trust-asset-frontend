import sbt.*

object AppDependencies {

  val bootstrapVersion = "8.6.0"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-30"            % "9.11.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping-play-30" % "2.0.0",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30"            % bootstrapVersion
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.scalatestplus" %% "scalacheck-1-17"        % "3.2.18.0",
    "org.jsoup"          % "jsoup"                  % "1.17.2",
    "wolfendale"        %% "scalacheck-gen-regexp"  % "0.1.2"
  ).map(_ % Test)

  def apply(): Seq[ModuleID]      = compile ++ test
}
