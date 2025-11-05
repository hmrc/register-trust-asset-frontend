package views.asset.money

import views.behaviours.ViewBehaviours
import views.html.asset.money.MoneyAnswersView

class MoneyAnswersViewSpec extends ViewBehaviours {

  private val prefix: String = "money.answers"
  private val index: Int     = 0

  "MoneyAnswers view" must {

    val view = viewFor[MoneyAnswersView](Some(emptyUserAnswers))

    val applyView = view.apply(index, fakeDraftId, Nil)(fakeRequest, messages)

    behave like normalPage(applyView, prefix)

    behave like pageWithBackLink(applyView)
  }
}
