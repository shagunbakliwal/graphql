```
{
  "query": "query ($request: [UserInput!]!) { validateDynamic(input: $request) { index inputType valid message invalidFields } }",
  "variables": {
    "request": [
      {
        "username": "JohnDoe",
        "email": "john@example.com",
        "age": "25"
      },
      {
        "username": "JaneDoe",
        "email": "jane@example.com",
        "age": "invalid_age"
      }
    ]
  }
}```