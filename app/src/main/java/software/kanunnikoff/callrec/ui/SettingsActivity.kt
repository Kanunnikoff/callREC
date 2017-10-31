package software.kanunnikoff.callrec.ui

import android.os.Bundle
import android.view.MenuItem
import software.kanunnikoff.callrec.R
import software.kanunnikoff.screenrec.ui.AppCompatPreferenceActivity

class SettingsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()

            return true
        }

        return super.onMenuItemSelected(featureId, item)
    }
}
