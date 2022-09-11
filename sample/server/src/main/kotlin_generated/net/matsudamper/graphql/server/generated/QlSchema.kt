// Generated by Main
package net.matsudamper.graphql.server.generated

import kotlin.String

public object QlSchema {
  public val text: String = """
      directive @lazy on FIELD_DEFINITION
      scalar UserId
      scalar JvmInt
      scalar JvmFloat
      scalar JvmDouble
      scalar Date
      scalar UserPostContentId

      schema {
          query: Query
      }
      
      type Query {
          timeline: TimelineContents
          user(user: UserId!) : User
      }
      
      type User {
          id: UserId!
          postContent(id: UserPostContentId!): [UserPostContent!]!
      }
      
      type UserPostContent {
          id: UserPostContentId!
          text: String!
          user: User! @lazy
          createDateAt: Date!
      }
      
      union TimelineContents = User | UserPostContent
      """.trimIndent()
}