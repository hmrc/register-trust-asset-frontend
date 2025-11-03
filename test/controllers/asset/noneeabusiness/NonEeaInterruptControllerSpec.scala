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

package controllers.asset.noneeabusiness

import base.SpecBase
import controllers.IndexValidation
import navigation.NonEeaBusinessNavigator
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.asset.noneeabusiness.NonEeaInterruptView

class NonEeaInterruptControllerSpec extends SpecBase with IndexValidation {

  private val index = 0

  "NonEeaInterruptController" must {

    "return OK and the correct view for a GET" in {

      val nonEeaInterruptControllerRoute: String = routes.NonEeaInterruptController.onPageLoad(0, fakeDraftId).url

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val request = FakeRequest(GET, nonEeaInterruptControllerRoute)

      val result = route(application, request).value

      val view = application.injector.instanceOf[NonEeaInterruptView]

      status(result) mustEqual OK

      contentAsString(result) mustEqual
        view(index, fakeDraftId)(request, messages).toString

      application.stop()
    }

    "redirect to the NameController when onSubmit called" in {

      val nonEeaInterruptControllerRoute: String = routes.NonEeaInterruptController.onSubmit(0, fakeDraftId).url

      val nameControllerRoute: String = routes.NameController.onPageLoad(0, fakeDraftId).url

      val navigator = app.injector.instanceOf[NonEeaBusinessNavigator]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), navigator = navigator).build()

      val request =
        FakeRequest(POST, nonEeaInterruptControllerRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual nameControllerRoute

      application.stop()
    }
  }

}
