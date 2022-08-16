package br.com.crearesistemas.shift_leader.ui.semi_mechanized

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import br.com.crearesistemas.shift_leader.R
import br.com.crearesistemas.shift_leader.db_service.model.Appointment
import br.com.crearesistemas.shift_leader.db_service.model.AppointmentSaved
import br.com.crearesistemas.shift_leader.db_service.model.AppointmentSemiMechanized
import br.com.crearesistemas.shift_leader.db_service.model.Machine
import br.com.crearesistemas.shift_leader.db_service.viewmodel.AppointmentSavedViewModel
import br.com.crearesistemas.shift_leader.db_service.viewmodel.AppointmentSemiMechanizedViewModel
import br.com.crearesistemas.shift_leader.db_service.viewmodel.AppointmentViewModel
import br.com.crearesistemas.shift_leader.db_service.viewmodel.MachineViewModel
import br.com.crearesistemas.shift_leader.dto.ChainsawBuckingDto
import br.com.crearesistemas.shift_leader.ui.util.DialogUtil
import java.util.*

class ChainsawBuckingFragment : Fragment() {

    val LOG_TAG = "Chainsaw_Bucking"

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_chainsaw_bucking, container, false)

        val buttonSave = root.findViewById<Button>(R.id.btn_save)

        val txtLocation = root.findViewById<EditText>(R.id.txt_location)
        val txtQuantityChainsawMen = root.findViewById<EditText>(R.id.txt_quantity_chainsaw_men)
        val txtAreaSize = root.findViewById<EditText>(R.id.txt_area_size)
        val txtWorkingHours = root.findViewById<EditText>(R.id.txt_working_hours)

        val userId = 1L

        buttonSave.setOnClickListener {
            DialogUtil().openSuccessDialog(requireContext())
            try {

                var chainsawBucking = ChainsawBuckingDto()
                chainsawBucking.location = txtLocation.text.toString()
                chainsawBucking.areaSize = txtAreaSize.text.toString().toFloat()
                chainsawBucking.quantityOfChainsawMen = Integer(txtQuantityChainsawMen.text.toString()).toInt()
                chainsawBucking.workingHours = txtWorkingHours.text.toString().toFloat()

                val appoint = AppointmentSemiMechanized().apply {

                    this.origin = "Semi_Mechanized"
                    this.type = "chainsaw_bucking"
                    this.data = chainsawBucking.toJson()
                    this.userId = userId
                    // collectedAt - add auto in repository
//                    this.collectedAt = OffsetDateTime.now()
                }

                val appointSemiMechanizedService = AppointmentSemiMechanizedViewModel(requireActivity().application)

                appointSemiMechanizedService.save(appoint)

                /*var machine = Machine()
                machine.description = "SECURITY_NETWORK"
                machine.hotspotPassword = "picapausapoanoesgalinha111"
                machine.hotspotSsid="SECURITY_NETWORK"
                var machineService = MachineViewModel(requireActivity().application)
                machineService.save(machine)
*/
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
