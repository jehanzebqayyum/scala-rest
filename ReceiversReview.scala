package models.reactive

import reactivemongo.bson.BSONObjectID

case class ReceiversReview(
  _id: BSONObjectID,
  sharingId: BSONObjectID,
  experience: String,
  communication: Int,
  care: Int,
  punctual: Int,
  recommended: Boolean)

