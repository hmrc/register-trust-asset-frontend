/*
 * Copyright 2021 HM Revenue & Customs
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

import config.FrontendAppConfig
import controllers.asset.routes.{AssetInterruptPageController, WhatKindOfAssetController}
import controllers.routes.SessionExpiredController
import models.WhatKindOfAsset.{Business, Money, NonEeaBusiness, Other, Partnership, PropertyOrLand, Shares}
import models.{AddAssets, UserAnswers}
import pages.Page
import pages.asset._
import play.api.mvc.Call
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HttpVerbs.GET

import javax.inject.Inject

class AssetNavigator @Inject()(config: FrontendAppConfig) extends Navigator {

  override def route(draftId: String): PartialFunction[Page, AffinityGroup => UserAnswers => Call] = {
    case TrustOwnsNonEeaBusinessYesNoPage => _ => ua => yesNoNav(
      ua = ua,
      fromPage = TrustOwnsNonEeaBusinessYesNoPage,
      yesCall = AssetInterruptPageController.onPageLoad(draftId),
      noCall = assetsCompletedRoute(draftId)
    )
    case AssetInterruptPage => _ => ua => routeToAssetIndex(ua, draftId)
    case WhatKindOfAssetPage(index) => _ => ua => whatKindOfAssetRoute(ua, index, draftId)
    case AddAssetsPage => _ => addAssetsRoute(draftId)
    case AddAnAssetYesNoPage => _ => ua => yesNoNav(
      ua = ua,
      fromPage = AddAnAssetYesNoPage,
      yesCall = routeToAssetIndex(ua, draftId),
      noCall = assetsCompletedRoute(draftId)
    )
  }

  def assetsCompletedRoute(draftId: String) : Call = {
    Call(GET, config.registrationProgressUrl(draftId))
  }

  private def addAssetsRoute(draftId: String)(answers: UserAnswers): Call = {
    val addAnother = answers.get(AddAssetsPage)

    addAnother match {
      case Some(AddAssets.YesNow) =>
        routeToAssetIndex(answers, draftId)
      case Some(AddAssets.YesLater) =>
        assetsCompletedRoute(draftId)
      case Some(AddAssets.NoComplete) =>
        assetsCompletedRoute(draftId)
      case _ => SessionExpiredController.onPageLoad()
    }
  }

  private def routeToAssetIndex(answers: UserAnswers, draftId: String): Call = {
    val assets = answers.get(sections.Assets).getOrElse(List.empty)

    if (answers.isTaxable) {
      val index = assets match {
        case Nil => 0
        case x if !x.last.isComplete => x.size - 1
        case x => x.size
      }
      WhatKindOfAssetController.onPageLoad(index, draftId)
    } else {
      // assets includes an in progress non-EEA business asset as we have just set the value in WhatKindOfAssetPage
      // therefore we need the index to correspond to that asset (i.e. assets.size - 1)
      val index = assets.size - 1
      controllers.asset.noneeabusiness.routes.NameController.onPageLoad(index, draftId)
    }
  }

  private def whatKindOfAssetRoute(answers: UserAnswers, index: Int, draftId: String): Call =
    answers.get(WhatKindOfAssetPage(index)) match {
      case Some(Money) =>
        controllers.asset.money.routes.AssetMoneyValueController.onPageLoad(index, draftId)
      case Some(PropertyOrLand) =>
        controllers.asset.property_or_land.routes.PropertyOrLandAddressYesNoController.onPageLoad(index, draftId)
      case Some(Shares) =>
        controllers.asset.shares.routes.SharesInAPortfolioController.onPageLoad(index, draftId)
      case Some(Business) =>
        controllers.asset.business.routes.BusinessNameController.onPageLoad(index, draftId)
      case Some(Partnership) =>
        controllers.asset.partnership.routes.PartnershipDescriptionController.onPageLoad(index, draftId)
      case Some(Other) =>
        controllers.asset.other.routes.OtherAssetDescriptionController.onPageLoad(index, draftId)
      case Some(NonEeaBusiness) =>
        controllers.asset.noneeabusiness.routes.NameController.onPageLoad(index, draftId)
      case _ =>
        SessionExpiredController.onPageLoad()
    }

}
