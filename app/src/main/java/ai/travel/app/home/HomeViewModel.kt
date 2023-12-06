package ai.travel.app.home

import ai.travel.app.dto.ApiPrompt
import ai.travel.app.dto.PalmApi
import ai.travel.app.dto.Prompt
import ai.travel.app.repository.ApiService
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val repository: ApiService,
) : AndroidViewModel(application) {

    private val _imageState = MutableStateFlow<ApiState>(ApiState.NotStarted)
    val imageState: StateFlow<ApiState> = _imageState.asStateFlow()

    private val _message = MutableStateFlow("")
    private val _location = MutableStateFlow("")
    private val _budget = MutableStateFlow("")
    private val noOfDays = MutableStateFlow("")

    private val _data = MutableStateFlow(emptyList<Map<String, String>>())
    val data: StateFlow<List<Map<String, String>>> = _data.asStateFlow()

    private val _geoCodesData = MutableStateFlow(emptyList<TourDetails>().toMutableList())
    val geoCodesData: StateFlow<List<TourDetails>> = _geoCodesData.asStateFlow()




    fun getApiData() {
        viewModelScope.launch {
            delay(1000)
            val apiData =
                repository.getApiData(
                    ApiPrompt(
                        prompt = Prompt(
                            text = _message.value
                        )
                    )
                )
            _imageState.value = ApiState.Loaded(apiData)
            extractTourDetails(apiData.candidates?.get(0)?.output ?: "")
            getGeoCodes()
        }
    }

    private fun getGeoCodes() {
        viewModelScope.launch {
            delay(1000)
            _data.value.forEachIndexed { index, location ->
                val geoCodes = mutableMapOf<String, String>()
                val day = location.getOrDefault("Day", "-2")
                if (day != "-2") {
                    val locationName = location.getOrDefault("Name", "")
                    if (locationName != "") {
                        val apiData =
                            repository.getGeocodingData(
                               query = "$locationName, ${_location.value}",
                            )
                        geoCodes["latitude"] = apiData.items?.get(0)?.position?.lat?.toString() ?: ""
                        geoCodes["longitude"] = apiData.items?.get(0)?.position?.lng?.toString() ?: ""
                        _geoCodesData.value[index].geoCode = GeoCode(
                            latitude = geoCodes["latitude"] ?: "",
                            longitude = geoCodes["longitude"] ?: ""
                        )
                    }
                }

            }
            _imageState.value = ApiState.ReceivedGeoCodes
        }
    }

    fun updateMessage(message: String, location: String, noOfDays: String) {
        _location.value = location
        this.noOfDays.value = noOfDays
        _message.value = message
        _imageState.value = ApiState.Loading
    }

    private fun extractTourDetails(output: String) {
        val tourDetails = mutableListOf<Map<String, String>>()

        val days = """Day (\d+) ([A-Za-z]+)""".toRegex(
            options = setOf(
                RegexOption.IGNORE_CASE
            )
        ).findAll(output)

        val names = """Name:(.{0,50})""".toRegex(
            options = setOf(
                RegexOption.IGNORE_CASE
            )
        ).findAll(output)

        val budgets = """Budget:(.{0,20})""".toRegex(
            options = setOf(
                RegexOption.IGNORE_CASE
            )
        ).findAll(output)

        println("Output: $output")

        println(
            "daysssss: ${
                days.forEachIndexed { index, matchResult ->
                    println("daysssss: $index, matchResult: ${matchResult.groupValues}")
                }
            }"
        )

        println(
            "names: ${
                names.forEachIndexed { index, matchResult ->
                    println("names: $index, matchResult: ${matchResult.groupValues}")
                }
            }"
        )

        println(
            "btssss: ${
                budgets.forEachIndexed { index, matchResult ->
                    println("btssss: $index, matchResult: ${matchResult.groupValues}")
                }
            }"
        )


        val namesList = names.map { it.groupValues[1] }.toList()
        val budgetsList = budgets.map { it.groupValues[1] }.toList()

        println("namesList: $namesList")
        println("budgetsList: $budgetsList")

        days.forEachIndexed { index, dayMatch ->
            val dayNumber = dayMatch.groupValues[1]
            val timeOfDay = dayMatch.groupValues[2]

            val dayInfo = mutableMapOf<String, String>()
            dayInfo["Day"] = dayNumber
            dayInfo["Time of Day"] = timeOfDay

            if (index < namesList.size) {
                dayInfo["Name"] = namesList[index]
            }

            if (index < budgetsList.size) {
                dayInfo["Budget"] = budgetsList[index]
            }

            tourDetails.add(dayInfo)
            _geoCodesData.value.add(
                TourDetails(
                    day = dayNumber,
                    timeOfDay = timeOfDay,
                    name = namesList[index],
                    budget = budgetsList[index]
                )
            )
        }

        _data.value = tourDetails

    }


}

sealed class ApiState {
    object Loading : ApiState()
    data class Loaded(val data: PalmApi) : ApiState()

    data class Error(val exception: Exception) : ApiState()

    object NotStarted : ApiState()
    object ReceivedGeoCodes : ApiState()
}

data class GeoCode(
    val latitude: String,
    val longitude: String
)

data class TourDetails(
    val day: String,
    val timeOfDay: String,
    val name: String,
    val budget: String,
    var geoCode: GeoCode? = null
)