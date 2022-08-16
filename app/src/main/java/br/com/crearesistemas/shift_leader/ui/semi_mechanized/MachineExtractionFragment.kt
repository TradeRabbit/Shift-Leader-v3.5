package br.com.crearesistemas.shift_leader.ui.semi_mechanized

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import br.com.crearesistemas.shift_leader.R
import br.com.crearesistemas.shift_leader.db_service.model.AppointmentSaved
import br.com.crearesistemas.shift_leader.db_service.model.AppointmentSemiMechanized
import br.com.crearesistemas.shift_leader.db_service.viewmodel.AppointmentSavedViewModel
import br.com.crearesistemas.shift_leader.db_service.viewmodel.AppointmentSemiMechanizedViewModel
import br.com.crearesistemas.shift_leader.dto.MachineExtractionDto
import br.com.crearesistemas.shift_leader.ui.util.DialogUtil
import java.util.*

class MachineExtractionFragment : Fragment() {

    val LOG_TAG = "Machine_Extraction"

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_machine_extraction, container, false)

        val buttonSave = root.findViewById<Button>(R.id.btn_save)

        val txtSizeVol = root.findViewById<EditText>(R.id.txt_size_vol)

        val userId = 1L

        buttonSave.setOnClickListener {
            DialogUtil().openSuccessDialog(requireContext())
           try {

                var machineExtraction = MachineExtractionDto()
                machineExtraction.sizeVolIsAPreset = txtSizeVol.text.toString().toFloat()


                val appoint = AppointmentSemiMechanized().apply {

                    this.origin = "Semi_Mechanized"
                    this.type = "Machine_Extraction"
                    this.data = machineExtraction.toJson()
                    this.userId = userId
                    // collectedAt - add auto in repository
//                    this.collectedAt = OffsetDateTime.now()
                }

                val appointSemiMechanizedService = AppointmentSemiMechanizedViewModel(requireActivity().application)

               appointSemiMechanizedService.save(appoint)

                DialogUtil().openSuccessDialog(requireContext())

            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(LOG_TAG,e.localizedMessage)
                DialogUtil().openErrorDialog(requireContext())
            }
        }

        return root
    }

}