package br.com.crearesistemas.shift_leader.ui.semi_mechanized

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import br.com.crearesistemas.shift_leader.R
import br.com.crearesistemas.shift_leader.db_service.model.AppointmentSaved
import br.com.crearesistemas.shift_leader.db_service.model.AppointmentSemiMechanized
import br.com.crearesistemas.shift_leader.db_service.viewmodel.AppointmentSavedViewModel
import br.com.crearesistemas.shift_leader.db_service.viewmodel.AppointmentSemiMechanizedViewModel
import br.com.crearesistemas.shift_leader.dto.MachinePrebunchingDto
import br.com.crearesistemas.shift_leader.ui.util.DialogUtil
import java.util.*

class MachinePrebunchingFragment : Fragment() {

    val LOG_TAG = "Machine_Prebunching"

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_machine_prebunching, container, false)

        val buttonSave = root.findViewById<Button>(R.id.btn_save)

        val txtLocation = root.findViewById<EditText>(R.id.txt_location)
        val txtActualSize = root.findViewById<EditText>(R.id.txt_actual_size)
        val txtAreaSize = root.findViewById<EditText>(R.id.txt_area_size)

        val userId = 1L

        buttonSave.setOnClickListener {
            DialogUtil().openSuccessDialog(requireContext())
            try {

                var machinePrebunching = MachinePrebunchingDto()
                machinePrebunching.location = txtLocation.text.toString()
                machinePrebunching.areaSize = txtAreaSize.text.toString().toFloat()
                machinePrebunching.actualSizePerformed = txtActualSize.text.toString().toFloat()

                val appoint = AppointmentSemiMechanized().apply {

                    // origin tem q ser o MAC ou algo q referencie este tablet,
                    // AppointmentSemiMechanized de todos os shiftleader vão para a mesma tabela na api
                    // lá é chave composta de id + origin (evitar conflitos de id vindos de tablets diferentes)
                    this.origin = "Semi_Mechanized"
                    this.type = "Machine_Prebunching"
                    this.data = machinePrebunching.toJson()
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