package com.example.snapeats.data.offline

/**
 * Hardcoded offline database of 50 common foods mapped to their approximate
 * calorie content per 100 g.
 *
 * Used as a fallback when the FatSecret API is unreachable (no network, timeout,
 * or HTTP error). Food names are all lowercase so matching can be done with a
 * simple [String.contains] check after lower-casing the query.
 */
object LocalFoodDatabase {

    val foods: Map<String, Int> = mapOf(
        // Fruits
        "apple" to 52,
        "banana" to 89,
        "orange" to 47,
        "grape" to 69,
        "strawberry" to 32,
        "watermelon" to 30,
        "mango" to 60,
        "pineapple" to 50,
        "blueberry" to 57,
        "peach" to 39,

        // Vegetables
        "salad" to 15,
        "lettuce" to 15,
        "spinach" to 23,
        "broccoli" to 34,
        "carrot" to 41,
        "potato" to 77,
        "sweet potato" to 86,
        "tomato" to 18,
        "cucumber" to 16,
        "onion" to 40,

        // Grains & Bread
        "rice" to 130,
        "pasta" to 131,
        "bread" to 265,
        "white bread" to 265,
        "brown bread" to 247,
        "oats" to 389,
        "cornflakes" to 357,
        "noodle" to 138,
        "noodles" to 138,
        "tortilla" to 237,

        // Proteins
        "chicken" to 165,
        "chicken breast" to 165,
        "beef" to 250,
        "pork" to 242,
        "fish" to 136,
        "salmon" to 208,
        "tuna" to 132,
        "egg" to 155,
        "tofu" to 76,
        "lentils" to 116,

        // Dairy
        "milk" to 61,
        "cheese" to 402,
        "yogurt" to 59,
        "butter" to 717,
        "cream" to 340,

        // Fast food & snacks
        "pizza" to 266,
        "burger" to 295,
        "french fries" to 312,
        "chips" to 536,
        "chocolate" to 546,
        "cookie" to 480,

        // Indian foods
        "butter naan" to 300,
        "naan" to 300,
        "roti" to 120,
        "chapati" to 120,
        "paratha" to 260,
        "aloo paratha" to 280,
        "biryani" to 200,
        "chicken biryani" to 210,
        "dal" to 120,
        "dal makhani" to 150,
        "dal fry" to 130,
        "paneer" to 265,
        "paneer butter masala" to 180,
        "palak paneer" to 160,
        "butter chicken" to 170,
        "chicken curry" to 175,
        "mutton curry" to 195,
        "samosa" to 260,
        "idli" to 58,
        "dosa" to 168,
        "masala dosa" to 210,
        "vada" to 195,
        "upma" to 145,
        "poha" to 130,
        "raita" to 60,
        "lassi" to 100,
        "khichdi" to 135,
        "pongal" to 150,
        "curd rice" to 140,
        "rasam" to 30,
        "sambhar" to 55,
        "chutney" to 50,
        "pickle" to 30,
        "papad" to 60,
        "tandoori chicken" to 165,
        "kebab" to 220,
        "halwa" to 350,
        "kheer" to 155,
        "gulab jamun" to 175,
        "jalebi" to 360,
        "puri" to 315,
        "bhatura" to 340,
        "chole" to 180,
        "rajma" to 140,
        "pulao" to 160
    )
}
