const CLIENT_API_FRIENDLY_NAME = "FoodSearchClientAPI"
var connectedSystem;

// Event callbacks
$(document).ready(function () {

    // on click of the foodItemBtn, call the Client API Connected System
    $('#foodItemBtn').on("click", getRecipe);
    Appian.Component.onNewValue(function (newValues) {
      var csId = newValues.connectedSystem;
      console.log("the csId is", csId)
      if (connectedSystem !== csId) {
        connectedSystem = csId;
      }
    });
});


function getRecipe() {
    if (!connectedSystem) {
        return;
    }
    const payload = { foodItem: document.getElementById("foodItem").value};
    Appian.Component.invokeClientApi(connectedSystem, CLIENT_API_FRIENDLY_NAME, payload)
    .then(handleClientApiResponse)
    .catch(handleError);
}

function handleError(response) {
    if (response.error && response.error[0]) {
        Appian.Component.setValidations([error.error]);
    } else {
        Appian.Component.setValidations(["An unspecified error occurred"]);
    }
}

function handleClientApiResponse(response) {
    if (response.payload.error) {
        Appian.Component.setValidations(response.payload.error);
        return;
    }
    // Clear any error messages
    Appian.Component.setValidations([]);
      // Clear previous content
      $('#food-information-div').empty();

      // Iterate through the meals array
      response.payload.meals.forEach(meal => {
          // Create HTML elements for each piece of data
          const mealDiv = $('<div></div>').addClass('meal');
          const mealTitle = $('<h4></h4>').text(meal.strMeal);
          const mealCategory = $('<p></p>').text(`Category: ${meal.strCategory}`);
          const mealArea = $('<p></p>').text(`Cuisine: ${meal.strArea}`);
          const mealInstructions = $('<p></p>').text(meal.strInstructions);
          const mealImage = $('<img>').attr('src', meal.strMealThumb).attr('alt', meal.strMeal);
          const newLine = $('<br>');
          const mealVideoLink = $('<a></a>').attr('href', meal.strYoutube).text('Watch Video');
          const mealSourceLink = $('<a></a>').attr('href', meal.strSource).text('View Source');

          // Append elements to the mealDiv
          mealDiv.append(mealTitle, mealCategory, mealArea, mealInstructions, mealImage, newLine, mealVideoLink, mealSourceLink);

          // Append the mealDiv to the food-information-div
          $('#food-information-div').append(mealDiv);
      });
}