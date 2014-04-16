package controllers

import play.api.mvc._
import play.api.libs.json._
import securesocial._
import models._
import models.reactive._
import dao.ReceiversReviewDAO
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import ModelsJsonFormats._
import securesocial.core.SecuredRequest

object ReceiversReviewController extends Controller with Security with MongoController with securesocial.core.SecureSocial {
  /*
  implicit val receiversReviewReads = (
    (__ \ "_id").read(BSONObjectID.generate) and
      (__ \ "sharingId").read(new BSONObjectID(Array[Byte](0))) and
      (__ \ "experience").read[String] and
      (__ \ "communication").read[Int] and
      (__ \ "care").read[Int] and
      (__ \ "punctual").read[Int] and
      (__ \ "recommended").read[Boolean]
    )(ReceiversReview)
  */
  def receiversReviewCollection: JSONCollection = db.collection[JSONCollection]("ReceiversReviews")

  def sharingCollection: JSONCollection = db.collection[JSONCollection]("Sharings")


  def create() = AuthorizedAction(true, parse.json, Role.GUEST) {
      user => implicit request =>
      request.body.validate[ReceiversReview].map {
        case review => {
          withSharing(review.sharingId.toString) {
            sharing => asGiver(sharing, user) {
              Async {
                ReceiversReviewDAO.insert(review).map {
                  case _ => Ok("Created Review id: " + review._id)
                }
              }
            }
          }
        }
      }.recoverTotal {
        e => BadRequest("error: " + JsError.toFlatJson(e))
      }
  }

  private def withReview(reviewId: String)(f: ReceiversReview => Result) = {
    Async {
      receiversReviewCollection.find(Json.obj("_id" -> reviewId)).cursor[ReceiversReview].headOption.map {
        case None => NotFound("Not found")
        case Some(review) => f(review)
      }
    }
  }

  private def withSharing(sharingId: String)(f: Sharing => Result) = {
    Async {
      sharingCollection.find(Json.obj("_id" -> sharingId)).cursor[Sharing].headOption.map {
        case None => NotFound("Not found")
        case Some(sharing) => f(sharing)
      }
    }
  }

  private def asGiver(sharing: Sharing, user: User)(f: => Result) = {
    if (sharing.giver._id.toString == user._id.toString) f
    else Forbidden("Not allowed")
  }

  private def isNotReviewed(sharingId: String)(f: => Result) = {
    Async {
      receiversReviewCollection.find(Json.obj("sharingId" -> sharingId)).cursor[ReceiversReview].headOption.map {
        case None => f
        case _ => Forbidden("Already reviewed")
      }
    }
  }
}


