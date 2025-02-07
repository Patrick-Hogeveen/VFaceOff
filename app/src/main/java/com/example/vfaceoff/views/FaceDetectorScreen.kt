import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vfaceoff.imageAnalysis.LandmarkDetectorComposeBuilder
import kotlinx.coroutines.CoroutineScope

@Composable
fun ScreenFaceDetector(
    context: Context,
    coroutineScope: CoroutineScope,
    //cameraPermissionRequest: PermissionResultRequest
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //Text(text = "Hello MediaPipe", style = MaterialTheme.typography.titleLarge)

        LandmarkDetectorComposeBuilder(
            context = context,
            scope = coroutineScope,
            //cameraPermissionRequest = cameraPermissionRequest
        ).Build(
            //modifier = Modifier.fillMaxWidth(0.9f),
            containerShape = RoundedCornerShape(12.dp)
        )
    }

}