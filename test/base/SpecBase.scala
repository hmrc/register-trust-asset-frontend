/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package base

import config.annotations._
import controllers.actions._
import models.UserAnswers
import navigation.{FakeNavigator, Navigator}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import repositories.RegistrationsRepository
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, Enrolments}
import views.ViewUtils

trait SpecBase
    extends PlaySpec
    with GuiceOneAppPerSuite
    with TryValues
    with ScalaFutures
    with IntegrationPatience
    with Mocked
    with FakeTrustsApp
    with Matchers
    with OptionValues {

  val defaultAppConfigurations: Map[String, Any] = Map(
    "auditing.enabled" -> false,
    "metrics.enabled" -> false,
    "play.filters.disabled" -> List("play.filters.csrf.CSRFFilter", "play.filters.csp.CSPFilter")
  )

  final val ENGLISH = "en"
  final val WELSH   = "cy"

  lazy val draftId: String        = "draftId"
  lazy val userInternalId: String = "internalId"
  lazy val fakeDraftId: String    = draftId

  def emptyUserAnswers: UserAnswers = UserAnswers(draftId, Json.obj(), internalAuthId = userInternalId)

  lazy val fakeNavigator: FakeNavigator = new FakeNavigator()

  private def fakeDraftIdAction(userAnswers: Option[UserAnswers]): FakeDraftIdRetrievalActionProvider =
    new FakeDraftIdRetrievalActionProvider(userAnswers)

  protected def applicationBuilder(
    userAnswers: Option[UserAnswers] = None,
    affinityGroup: AffinityGroup = AffinityGroup.Organisation,
    enrolments: Enrolments = Enrolments(Set.empty[Enrolment]),
    navigator: Navigator = fakeNavigator
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[Navigator].toInstance(navigator),
        bind[Navigator].qualifiedWith(classOf[Asset]).toInstance(navigator),
        bind[Navigator].qualifiedWith(classOf[Money]).toInstance(navigator),
        bind[Navigator].qualifiedWith(classOf[PropertyOrLand]).toInstance(navigator),
        bind[Navigator].qualifiedWith(classOf[Shares]).toInstance(navigator),
        bind[Navigator].qualifiedWith(classOf[Business]).toInstance(navigator),
        bind[Navigator].qualifiedWith(classOf[Partnership]).toInstance(navigator),
        bind[Navigator].qualifiedWith(classOf[Other]).toInstance(navigator),
        bind[Navigator].qualifiedWith(classOf[NonEeaBusiness]).toInstance(navigator),
        bind[RegistrationDataRequiredAction].to[RegistrationDataRequiredActionImpl],
        bind[RegistrationIdentifierAction].toInstance(
          new FakeIdentifyForRegistration(affinityGroup, frontendAppConfig)(injectedParsers, trustsAuth, enrolments)
        ),
        bind[DraftIdRetrievalActionProvider].toInstance(fakeDraftIdAction(userAnswers)),
        bind[RegistrationsRepository].toInstance(registrationsRepository),
        bind[AffinityGroup].toInstance(Organisation),
        bind[ViewUtils].toInstance(mockViewUtils)
      )
      .configure(defaultAppConfigurations)
}
