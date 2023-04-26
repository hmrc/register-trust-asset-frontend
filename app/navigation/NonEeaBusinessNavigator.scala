/*
 * Copyright 2023 HM Revenue & Customs
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

package navigation

import controllers.asset.noneeabusiness.routes._
import models.UserAnswers
import pages.Page
import pages.asset.noneeabusiness._
import play.api.mvc.Call
import uk.gov.hmrc.auth.core.AffinityGroup

import javax.inject.Singleton

@Singleton
class NonEeaBusinessNavigator extends Navigator {

  override protected def route(draftId: String): PartialFunction[Page, AffinityGroup => UserAnswers => Call] = {
    case NamePage(index)                 => _ => _ => InternationalAddressController.onPageLoad(index, draftId)
    case InternationalAddressPage(index) => _ => _ => GoverningCountryController.onPageLoad(index, draftId)
    case GoverningCountryPage(index)     => _ => _ => StartDateController.onPageLoad(index, draftId)
    case StartDatePage(index)            => _ => _ => AnswersController.onPageLoad(index, draftId)
  }

}
