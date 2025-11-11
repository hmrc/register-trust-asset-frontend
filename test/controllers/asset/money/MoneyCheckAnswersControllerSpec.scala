/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.asset.money

import base.SpecBase
import models.Status.Completed
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.AssetStatus
import pages.asset.money._
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.print.MoneyPrintHelper
import views.html.asset.money.MoneyAnswersView

import scala.concurrent.Future

class MoneyCheckAnswersControllerSpec extends SpecBase {

  private val index: Int = 0
  val totalValue: Long   = 4000L
  lazy val answersRoute  = routes.MoneyCheckAnswersController.onPageLoad(index, fakeDraftId).url

  val baseAnswers = emptyUserAnswers
    .set(AssetMoneyValuePage(index), totalValue)
    .success
    .value
    .set(AssetStatus(index), Completed)
    .success
    .value

  "MoneyCheckAnswersController Controller" must {

    "return OK and the correct view for a GET" in {

      val expectedSections                  = Nil
      val mockPrintHelper: MoneyPrintHelper = mock[MoneyPrintHelper]()
      when(mockPrintHelper.checkDetailsSection(any(), any(), any(), any())(any())).thenReturn(Nil)

      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .overrides(bind[MoneyPrintHelper].toInstance(mockPrintHelper))
        .build()

      val request = FakeRequest(GET, answersRoute)

      val result: Future[Result] = route(application, request).value

      val view: MoneyAnswersView = application.injector.instanceOf[MoneyAnswersView]

      status(result) mustEqual OK

      contentAsString(result) mustEqual
        view(index, fakeDraftId, expectedSections)(request, messages).toString

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      val request = FakeRequest(POST, routes.MoneyCheckAnswersController.onSubmit(index, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.AddAssetsController.onPageLoad(fakeDraftId).url

      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, answersRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad.url

      application.stop()
    }

  }
}
