package com.reedcouk.jobs.screens.jobs.details

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.ImageSpan
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.core.text.HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.chip.Chip
import com.reedcouk.jobs.R
import com.reedcouk.jobs.components.analytics.AnalyticEvents
import com.reedcouk.jobs.components.analytics.AnalyticsEventType
import com.reedcouk.jobs.components.analytics.AnalyticsScreenNames
import com.reedcouk.jobs.components.analytics.common.trackSignInMethod
import com.reedcouk.jobs.components.analytics.logAnalyticsEvent
import com.reedcouk.jobs.components.extensions.EMPTY_STRING
import com.reedcouk.jobs.components.extensions.formatAppliedOnDate
import com.reedcouk.jobs.components.thirdparty.getMap
import com.reedcouk.jobs.components.thirdparty.glide.ImageLoader
import com.reedcouk.jobs.components.thirdparty.glide.onResourceReady
import com.reedcouk.jobs.components.ui.ExpandableTextView
import com.reedcouk.jobs.components.ui.FadeInAfterToolbarCollapse
import com.reedcouk.jobs.components.ui.GenericLoadingDialog
import com.reedcouk.jobs.components.ui.showGenericLoadingDialog
import com.reedcouk.jobs.components.ui.snackbar.ReedSnackBarDuration
import com.reedcouk.jobs.components.ui.snackbar.showBigSnackbar
import com.reedcouk.jobs.components.ui.snackbar.showOfflineSnackbar
import com.reedcouk.jobs.components.ui.snackbar.showSmallSnackbar
import com.reedcouk.jobs.components.ui.snackbar.showSomethingWentWrongSnackbar
import com.reedcouk.jobs.components.ui.snackbar.showSuccessfulSnackBar
import com.reedcouk.jobs.components.ui.snackbar.showUnsuccessfulBigSnackBar
import com.reedcouk.jobs.core.auth.AuthViewModel
import com.reedcouk.jobs.core.auth.AuthenticationSignInType
import com.reedcouk.jobs.core.coroutines.COMPUTATION_DISPATCHER
import com.reedcouk.jobs.core.coroutines.viewScope
import com.reedcouk.jobs.core.extensions.gone
import com.reedcouk.jobs.core.extensions.handle
import com.reedcouk.jobs.core.extensions.visible
import com.reedcouk.jobs.core.extensions.visibleOrGone
import com.reedcouk.jobs.core.kotlin.exaustive
import com.reedcouk.jobs.core.lifecycle.LinkToObjectWithLifecycle
import com.reedcouk.jobs.core.lifecycle.dismissDialog
import com.reedcouk.jobs.core.lifecycle.wrapWithRespectToLifecycle
import com.reedcouk.jobs.core.navigation.navigateSafe
import com.reedcouk.jobs.core.navigation.result.handleResult
import com.reedcouk.jobs.core.ui.BaseFragment
import com.reedcouk.jobs.core.ui.utils.ToolbarConfiguration
import com.reedcouk.jobs.core.ui.utils.configuration
import com.reedcouk.jobs.core.ui.utils.openEmail
import com.reedcouk.jobs.core.ui.utils.openUri
import com.reedcouk.jobs.core.ui.utils.waitForNextGlobalLayout
import com.reedcouk.jobs.databinding.FragmentJobDetailsBinding
import com.reedcouk.jobs.screens.jobs.application.ApplicationJourneyScreenResult
import com.reedcouk.jobs.screens.jobs.application.ApplicationProcessState
import com.reedcouk.jobs.screens.jobs.application.ApplicationProcessStep
import com.reedcouk.jobs.screens.jobs.application.MessageToApplicationJourney
import com.reedcouk.jobs.screens.jobs.application.StartApplicationResult
import com.reedcouk.jobs.screens.jobs.application.UserCameToJobFrom
import com.reedcouk.jobs.screens.jobs.application.postMessageToApplicationJourney
import com.reedcouk.jobs.screens.jobs.application.profile.PostRegistrationProfileFragmentArgs
import com.reedcouk.jobs.screens.jobs.application.profile.PostRegistrationProfileState
import com.reedcouk.jobs.screens.jobs.application.questions.ApplicationQuestionsFragmentArgs
import com.reedcouk.jobs.screens.jobs.application.submit.SubmitApplicationFragmentArgs
import com.reedcouk.jobs.screens.jobs.data.ApplicationStatus
import com.reedcouk.jobs.screens.jobs.data.BrandedJobAppearance
import com.reedcouk.jobs.screens.jobs.data.BrandedJobMediaItem
import com.reedcouk.jobs.screens.jobs.data.Job
import com.reedcouk.jobs.screens.jobs.data.JobState
import com.reedcouk.jobs.screens.jobs.data.mapEmploymentForm
import com.reedcouk.jobs.screens.jobs.details.analytics.ScreenNameEnum
import com.reedcouk.jobs.screens.jobs.details.apply.ApplyForJobUiState
import com.reedcouk.jobs.screens.jobs.details.similar.SimilarJobsAdapter
import com.reedcouk.jobs.screens.jobs.details.ui.ImprovedBulletSpan
import com.reedcouk.jobs.screens.jobs.formatDayPosted
import com.reedcouk.jobs.screens.jobs.formatJobType
import com.reedcouk.jobs.screens.jobs.loadJobLogo
import com.reedcouk.jobs.screens.jobs.search.analytics.RecyclerViewHorizontalSwipeListener
import com.reedcouk.jobs.screens.manage.profile.ProfileSuccessfullyUpdatedResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import java.util.*

@SuppressWarnings("LargeClass")
class JobDetailsFragment : BaseFragment() {

    override val layoutId = R.layout.fragment_job_details

    private var screenNameState = ScreenNameEnum.DEFAULT
    override val screenName: String get() = screenNameState.screenName

    private val arguments by navArgs<JobDetailsFragmentArgs>()
    private val viewModel by viewModel<JobDetailsViewModel> {
        parametersOf(arguments.jobId, arguments.showSimilarJobs, arguments.source)
    }
    private val computationDispatcher by inject<CoroutineDispatcher>(named(COMPUTATION_DISPATCHER))
    private val imageLoader by inject<ImageLoader>()

    private val isApplicationJourneyCurrentlyRunning get() = findNavController().currentDestination?.id != R.id.jobDetailsFragment

    private var loadingDialog: LinkToObjectWithLifecycle<GenericLoadingDialog>? = null
    private var withdrawConfirmationDialog: LinkToObjectWithLifecycle<WithdrawApplicationDialog>? = null

    private val binding by viewBinding(FragmentJobDetailsBinding::bind)

    private val similarJobsHorizontalSwipeListener = RecyclerViewHorizontalSwipeListener {
        val isNeedToFireSimilarJobsSwipeEvent = viewModel.onSimilarJobsRightSwipe()
        if (isNeedToFireSimilarJobsSwipeEvent) {
            logAnalyticsEvent(AnalyticEvents.JobDetails.SIMILAR_JOB_CAROUSEL_SWIPE, AnalyticsEventType.SWIPE)
        }
    }

    private val brandedMediaHorizontalSwipeListener = RecyclerViewHorizontalSwipeListener {
        val isNeedToFireBrandedMediaSwipeEvent = viewModel.onBrandedMediaRightSwipe()
        if (isNeedToFireBrandedMediaSwipeEvent) {
            logAnalyticsEvent(AnalyticEvents.JobDetails.MEDIA_CAROUSEL_SWIPE, AnalyticsEventType.SWIPE)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.configuration = ToolbarConfiguration(
            homeClick = { goBackAfterArrowButtonClickWithRespectToApplicationJourney() }
        )
        binding.brandedBackButton.setOnClickListener { goBackAfterArrowButtonClickWithRespectToApplicationJourney() }
        viewScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    JobDetailsViewModel.State.Error -> showError()
                    JobDetailsViewModel.State.Loading -> showLoading()
                    is JobDetailsViewModel.State.DataReady -> updateJobDetails(state)
                }
            }
        }

        viewModel.events.handle(viewLifecycleOwner, ::handleEvent)

        binding.jobDetailsContentRoot.saveButton.setOnClickListener { view ->
            saveJob(view!!)
        }
        binding.jobDetailsContentRoot.hideButton.setOnClickListener {
            if (isApplicationJourneyCurrentlyRunning) {
                postMessageToApplicationJourney(MessageToApplicationJourney.UserWantToLeaveJobDetails(LeaveJobDetailsReason.HideJob))
            } else {
                viewScope.launch {
                    val state = viewModel.state.value
                    if (state is JobDetailsViewModel.State.DataReady && state.job.jobState == JobState.HIDDEN) {
                        logAnalyticsEvent(AnalyticEvents.JobDetails.UNHIDE_JOB_TAPPED, AnalyticsEventType.TAP)
                        viewModel.hideJob()
                        logAnalyticsEvent(AnalyticEvents.JobDetails.UNHIDE_JOB, AnalyticsEventType.KEY)
                    } else {
                        logAnalyticsEvent(AnalyticEvents.JobDetails.DISCARD_JOB_TAPPED, AnalyticsEventType.TAP)
                        viewModel.hideJob()
                        logAnalyticsEvent(AnalyticEvents.JobDetails.DISCARD_JOB_KEY, AnalyticsEventType.KEY)
                        goBack()
                    }
                }
            }
        }

        binding.jobDetailsWithdrawTextView.setOnClickListener {
            logAnalyticsEvent(AnalyticEvents.JobDetails.WITHDRAW_TAPPED, AnalyticsEventType.TAP)
            viewModel.onWithdrawClicked()
        }

        setupGoogleMap()
        setupSimilarJobs()
        handleApplicationJourneyResult()

        binding.jobDetailsContentRoot.similarJobsRecyclerView.addOnScrollListener(similarJobsHorizontalSwipeListener)
        binding.jobDetailsContentRoot.brandedMedia.addOnScrollListener(brandedMediaHorizontalSwipeListener)

        observeAuth()
        observeProfileSuccessfullyUpdated()
        observerTrainingCourseNoteClicks()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding.jobDetailsContentRoot.similarJobsRecyclerView.removeOnScrollListener(similarJobsHorizontalSwipeListener)
        binding.jobDetailsContentRoot.brandedMedia.removeOnScrollListener(brandedMediaHorizontalSwipeListener)
    }

    private fun updateJobDetails(state: JobDetailsViewModel.State.DataReady) {
        showJobDetails(state.job)
        updateAppliedStateRelatedUi(state.applyForJobUiState)
        updateWithdrawProcessUi(state.withdrawProcessState)
        updateTrainingCourseNoteUi(state.trainingCourseNoteState)
    }

    private fun goBackAfterArrowButtonClickWithRespectToApplicationJourney() {
        logAnalyticsEvent(AnalyticEvents.JobDetails.BACK_ARROW_TAPPED, AnalyticsEventType.TAP)
        if (isApplicationJourneyCurrentlyRunning) {
            postMessageToApplicationJourney(MessageToApplicationJourney.UserWantToLeaveJobDetails(LeaveJobDetailsReason.ExitFromJobDetails))
        } else {
            goBack()
        }
    }

    private fun updateWithdrawProcessUi(withdrawProcessState: JobDetailsViewModel.WithdrawProcessState): Unit = when (withdrawProcessState) {
        JobDetailsViewModel.WithdrawProcessState.Loading -> {
            loadingDialog = showGenericLoadingDialog(getString(R.string.loading))
            hideWithdrawConfirmationDialog()
        }
        JobDetailsViewModel.WithdrawProcessState.ConfirmingWithdraw -> {
            loadingDialog.dismissDialog()
            showWithdrawConfirmationDialog()
        }
        JobDetailsViewModel.WithdrawProcessState.NotRunning -> {
            hideWithdrawConfirmationDialog()
            loadingDialog.dismissDialog()
        }
    }

    private fun updateTrainingCourseNoteUi(trainingCourseNoteState: JobDetailsViewModel.TrainingCourseNoteState) {
        binding.jobDetailsContentRoot.jobDetailsTrainingCourseHideButton.visibleOrGone(
            trainingCourseNoteState == JobDetailsViewModel.TrainingCourseNoteState.Visible.TrainingCourse
        )
        exaustive - when (trainingCourseNoteState) {
            JobDetailsViewModel.TrainingCourseNoteState.Invisible -> binding.jobDetailsContentRoot.jobDetailsTrainingCourseGroup.gone()
            is JobDetailsViewModel.TrainingCourseNoteState.Visible -> {
                binding.jobDetailsContentRoot.jobDetailsTrainingCourseGroup.visible()
                binding.jobDetailsContentRoot.jobDetailsTrainingCourseNoteTitle.setText(
                    when (trainingCourseNoteState) {
                        JobDetailsViewModel.TrainingCourseNoteState.Visible.TrainingCourse -> R.string.trainingCourseTitle
                        JobDetailsViewModel.TrainingCourseNoteState.Visible.TrainingCourseHidden -> R.string.trainingCourseHiddenTitle
                    }
                )
                binding.jobDetailsContentRoot.jobDetailsTrainingCourseNoteDescription.setText(
                    when (trainingCourseNoteState) {
                        JobDetailsViewModel.TrainingCourseNoteState.Visible.TrainingCourse -> R.string.trainingCourseDescription
                        JobDetailsViewModel.TrainingCourseNoteState.Visible.TrainingCourseHidden -> R.string.trainingCourseHiddenDescription
                    }
                )
            }
        }
    }

    private fun observerTrainingCourseNoteClicks() {
        binding.jobDetailsContentRoot.jobDetailsTrainingCourseCloseButton.setOnClickListener {
            viewModel.onCloseTrainingCourseNoteClicked()
        }

        binding.jobDetailsContentRoot.jobDetailsTrainingCourseHideButton.setOnClickListener {
            viewModel.onHideTrainingJobsClicked()
        }
    }

    private fun showWithdrawConfirmationDialog() {
        withdrawConfirmationDialog = WithdrawApplicationDialog(requireContext(), object : WithdrawDialogCallback {
            override fun withdrawConfirmed() {
                viewModel.userConfirmedWithdraw()
            }

            override fun withdrawCancelled() {
                viewModel.userCancelledWithdraw()
            }
        }).apply {
            show()
        }.wrapWithRespectToLifecycle(viewLifecycleOwner)
    }

    private fun hideWithdrawConfirmationDialog() {
        withdrawConfirmationDialog.dismissDialog()
    }

    private fun handleApplicationJourneyResult() {
        viewScope.launchWhenStarted {
            findNavController().handleResult<ApplicationJourneyScreenResult>(
                viewLifecycleOwner,
                R.id.jobDetailsFragment,
                R.id.submitApplicationScreen,
                R.id.applicationQuestions
            ) { result ->
                when (result) {
                    is ApplicationJourneyScreenResult.LeaveParentScreen -> {
                        goBack()
                    }
                    is ApplicationJourneyScreenResult.UserConfirmedLeavingJobDetailsScreen -> when (result.leavingReason) {
                        is LeaveJobDetailsReason.HideJob -> {
                            viewScope.launch {
                                val state = viewModel.state.value
                                if (state is JobDetailsViewModel.State.DataReady && state.job.jobState == JobState.HIDDEN) {
                                    logAnalyticsEvent(AnalyticEvents.JobDetails.UNHIDE_JOB_TAPPED, AnalyticsEventType.TAP)
                                    viewModel.hideJob()
                                    logAnalyticsEvent(AnalyticEvents.JobDetails.UNHIDE_JOB, AnalyticsEventType.KEY)
                                } else {
                                    logAnalyticsEvent(AnalyticEvents.JobDetails.DISCARD_JOB_TAPPED, AnalyticsEventType.TAP)
                                    viewModel.hideJob()
                                    logAnalyticsEvent(AnalyticEvents.JobDetails.DISCARD_JOB_KEY, AnalyticsEventType.KEY)
                                    goBack()
                                }
                            }
                        }
                        is LeaveJobDetailsReason.NavigateToSimilarJob -> navigateToSimilarJob(result.leavingReason.jobId)
                        is LeaveJobDetailsReason.ExitFromJobDetails -> goBack()
                    }
                    is ApplicationJourneyScreenResult.ApplicationJourneyStepCompleted -> {
                        viewModel.applicationJourneyStepCompleted(result.application)
                    }
                    ApplicationJourneyScreenResult.InterruptedBecauseOfError -> showSomethingWentWrongSnackbar(
                        view = binding.coordinatorLayout,
                        anchorView = binding.setupJobAlertToastAnchor
                    )
                }
            }
        }
    }

    private fun handleEvent(event: JobDetailsViewModel.ScreenEvents): Unit = when (event) {
        is JobDetailsViewModel.ScreenEvents.StartedApplicationJourney -> handleStartApplicationJourneyResult(event.startApplicationProcessResult)
        JobDetailsViewModel.ScreenEvents.SomethingWentWrong -> showSomethingWentWrongSnackbar(
            binding.coordinatorLayout,
            binding.setupJobAlertToastAnchor
        )
        JobDetailsViewModel.ScreenEvents.ShowOfflineMessage -> showOfflineSnackbar(binding.coordinatorLayout, binding.setupJobAlertToastAnchor)
        JobDetailsViewModel.ScreenEvents.ApplicationWithdrawn -> showSuccessfullyWithdrawnToast()
        is JobDetailsViewModel.ScreenEvents.NextApplicationJourneyStep -> showNextApplicationJourneyStep(event.state)
        JobDetailsViewModel.ScreenEvents.ShowNoLongerHiddenMessage -> showNoLongerHiddenMessage()
        JobDetailsViewModel.ScreenEvents.ShowNoLongerSavedMessage -> showNoLongerSavedMessage()
        is JobDetailsViewModel.ScreenEvents.OpenMapWithOriginPoint -> openMapWithOriginPoint(event.location, event.postcode)
        is JobDetailsViewModel.ScreenEvents.OpenMapWithoutOriginPoint -> openMapWithoutOriginPoint(event.location)
    }

    private fun showNoLongerSavedMessage() {
        showSmallSnackbar(
            view = requireView(),
            duration = ReedSnackBarDuration.SHORT,
            icon = R.drawable.ic_check,
            iconTintColor = resources.getColor(R.color.successTickColor, null),
            primaryText = getString(R.string.unSaveJobActionSuccessMessage)
        )
    }

    private fun showNoLongerHiddenMessage() {
        showSmallSnackbar(
            view = requireView(),
            duration = ReedSnackBarDuration.SHORT,
            icon = R.drawable.ic_check,
            iconTintColor = resources.getColor(R.color.successTickColor, null),
            primaryText = getString(R.string.unHideJobActionSuccessMessage)
        )
    }

    private fun showSuccessfullyWithdrawnToast() {
        showBigSnackbar(
            binding.coordinatorLayout,
            getString(R.string.withdrawSuccessfulHeader),
            getString(R.string.withdrawSuccessfulBody),
            icon = R.drawable.ic_check,
            iconTintColor = resources.getColor(R.color.shade_02, null),
            anchorView = binding.setupJobAlertToastAnchor
        )
    }

    private fun handleStartApplicationJourneyResult(startApplicationResult: StartApplicationResult): Unit = when (startApplicationResult) {
        is StartApplicationResult.ApplicationProcessStarted -> showNextApplicationJourneyStep(startApplicationResult.state)
        StartApplicationResult.FailedToStartApplication.UserNotSignedIn -> {
            viewModel.signInClicked(AuthenticationSignInType.SIGN_IN)
        }
        StartApplicationResult.FailedToStartApplication.ProfileIsNotCompleted -> {
            findNavController().navigateSafe(
                R.id.action_jobDetailsFragment_to_profilePopupFragment,
                PostRegistrationProfileFragmentArgs(PostRegistrationProfileState.FillUpProfile).toBundle()
            )
        }

        StartApplicationResult.FailedToStartApplication.NotEligibleToApplyForThisJob -> {
            showNotEligibleJobUI()
            showNotEligibleSnackBar()
        }
    }

    private fun showNotEligibleSnackBar() {
        logAnalyticsEvent(AnalyticEvents.JobDetails.NOT_ELIGIBLE_TO_APPLY, AnalyticsEventType.KEY)
        showUnsuccessfulBigSnackBar(
            binding.coordinatorLayout,
            getString(R.string.not_eligible_primary_text),
            getString(R.string.not_eligible_secondary_text),
            binding.notEligibleTextView
        )
    }

    private fun showNextApplicationJourneyStep(state: ApplicationProcessState): Unit = when (state.nextStep) {
        is ApplicationProcessStep.ShowScreeningQuestions -> {
            findNavController().navigateSafe(
                R.id.action_jobDetailsFragment_to_applicationQuestions,
                ApplicationQuestionsFragmentArgs(
                    state.nextStep.questions.toTypedArray(),
                    state.application
                ).toBundle()
            )
        }
        ApplicationProcessStep.SubmitApplication -> {
            findNavController().navigateSafe(
                R.id.action_jobDetailsFragment_to_submitApplicationScreen,
                SubmitApplicationFragmentArgs(
                    application = state.application,
                    userCameFrom = this.arguments.source
                ).toBundle()
            )
        }
        ApplicationProcessStep.SuccessfulCompleted.NotifySuccessfullyCompleted -> showSuccessfulSnackBar(
            binding.coordinatorLayout,
            getString(R.string.submitApplicationSuccess),
            binding.setupJobAlertToastAnchor
        )
        is ApplicationProcessStep.SuccessfulCompleted.RedirectUserToUrl -> {
            if (!openUri(Uri.parse(state.nextStep.url))) {
                showSomethingWentWrongSnackbar(binding.coordinatorLayout, binding.setupJobAlertToastAnchor)
            }
            Unit
        }
    }

    private fun setupSimilarJobs() {
        binding.jobDetailsContentRoot.similarJobsRecyclerView.adapter = SimilarJobsAdapter { job ->
            logJobCardTappedAnalyticsEvent(job)
            userClickedOnSimilarJob(job.jobId)
        }
        binding.jobDetailsContentRoot.similarJobsRecyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

        viewModel.similarJobs.observe(viewLifecycleOwner) {
            when (it) {
                is JobDetailsViewModel.SimilarJobsState.Show -> showSimilarJobs(it.jobs)
                is JobDetailsViewModel.SimilarJobsState.DoNotShow -> hideSimilarJobs()
            }
        }
    }

    private fun userClickedOnSimilarJob(jobId: Long) {
        if (isApplicationJourneyCurrentlyRunning) {
            postMessageToApplicationJourney(MessageToApplicationJourney.UserWantToLeaveJobDetails(LeaveJobDetailsReason.NavigateToSimilarJob(jobId)))
        } else {
            navigateToSimilarJob(jobId)
        }
    }

    private fun navigateToSimilarJob(jobId: Long) {
        val currentSimilarJobs = viewModel.similarJobs.value
        if (currentSimilarJobs is JobDetailsViewModel.SimilarJobsState.Show && currentSimilarJobs.jobs.any { it.jobId == jobId }) {
            findNavController().navigateSafe(
                R.id.action_jobDetailsFragment_self,
                JobDetailsFragmentArgs(
                    jobId,
                    showSimilarJobs = false,
                    source = UserCameToJobFrom.SIMILAR_JOBS_JOB_DETAILS
                ).toBundle()
            )
        } else {
            showSomethingWentWrongSnackbar(binding.coordinatorLayout, binding.setupJobAlertToastAnchor)
        }
    }

    private fun logJobCardTappedAnalyticsEvent(job: Job) {
        val jobCardNumber = (binding.jobDetailsContentRoot.similarJobsRecyclerView.adapter as SimilarJobsAdapter).content.indexOf(job) + 1
        val jobCardNumberBundle = mapOf(AnalyticEvents.JobDetails.SIMILAR_JOB_CARD_NUMBER_PARAM to jobCardNumber)
        logAnalyticsEvent(AnalyticEvents.JobDetails.SIMILAR_JOB_TAPPED, AnalyticsEventType.TAP, jobCardNumberBundle)
    }

    override fun onResume() {
        super.onResume()
        viewModel.userEnteredScreen()
    }

    private fun hideSimilarJobs() {
        binding.jobDetailsContentRoot.similarJobsGroup.gone()
    }

    private fun showSimilarJobs(jobs: List<Job>) {
        binding.jobDetailsContentRoot.similarJobsGroup.visible()
        (binding.jobDetailsContentRoot.similarJobsRecyclerView.adapter as SimilarJobsAdapter).content = jobs
    }

    private fun setupGoogleMap() {
        val googleMapsFragment = childFragmentManager.findFragmentById(R.id.jobMapFragment) as SupportMapFragment
        viewScope.launch {
            val mapDiffered = async { googleMapsFragment.getMap() }
            viewModel.state.collect { state ->
                if (state is JobDetailsViewModel.State.DataReady && state.job.location != null) {
                    binding.jobDetailsContentRoot.mapsGroup.visible()
                    val location = state.job.location.let { LatLng(it.latitude, it.longitude) }
                    showLocationOnMap(mapDiffered.await(), location)
                } else {
                    binding.jobDetailsContentRoot.mapsGroup.gone()
                }
            }
        }
    }

    private fun updateAppliedStateRelatedUi(applyForJobUiState: ApplyForJobUiState) {
        exaustive - when (applyForJobUiState) {
            is ApplyForJobUiState.AppliedOn -> {
                showAppliedJobUI(applyForJobUiState)
            }
            is ApplyForJobUiState.Withdrawn -> {
                showWithdrawnJobUI(applyForJobUiState)
            }
            is ApplyForJobUiState.NotEligibleToApply -> {
                showNotEligibleJobUI()
            }
            else -> {
                showRegularJobUI(applyForJobUiState)
            }
        }

        binding.applyButton.isEnabled = applyForJobUiState is ApplyForJobUiState.ReadyToApply
        binding.applyButton.setOnClickListener {
            if (applyForJobUiState is ApplyForJobUiState.ReadyToApply) {
                val event = when (applyForJobUiState) {
                    ApplyForJobUiState.ReadyToApply.External -> AnalyticEvents.JobDetails.APPLY_ON_EXTERNAL_SITE_TAPPED
                    ApplyForJobUiState.ReadyToApply.Internal -> AnalyticEvents.JobDetails.APPLY_TAPPED
                }
                logAnalyticsEvent(event, AnalyticsEventType.TAP)
                viewModel.applyForAJob()
            }
        }
    }

    private fun showNotEligibleJobUI() {
        binding.jobDetailsContentRoot.saveButton.visible()
        binding.jobDetailsContentRoot.hideButton.visible()
        binding.jobDetailsContentRoot.scrollingSpace.visible()
        binding.jobDetailsContentRoot.jobDetailsAppliedJobScrollingSpace.gone()

        binding.jobDetailsAppliedDividerView.gone()
        binding.jobDetailsAppliedOnWithdrawnSpace.gone()
        binding.jobDetailsWithdrawTextView.gone()
        binding.jobDetailsContactTextView.gone()
        binding.jobDetailsAppliedJobGroup.gone()
        binding.applyButton.gone()

        binding.notEligibleTextView.visible()

        val label = SpannableString(getString(R.string.applyButtonNotEligible))
        label.setSpan(
            ImageSpan(
                requireContext(), R.drawable.ic_info,
                ImageSpan.ALIGN_BOTTOM
            ), label.length - 1, label.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.notEligibleTextView.text = label
        binding.notEligibleTextView.setOnClickListener {
            showNotEligibleSnackBar()
        }
    }

    private fun showRegularJobUI(applyForJobUiState: ApplyForJobUiState) {
        binding.jobDetailsContentRoot.saveButton.visible()
        binding.jobDetailsContentRoot.hideButton.visible()
        binding.jobDetailsContentRoot.scrollingSpace.visible()
        binding.jobDetailsContentRoot.jobDetailsAppliedJobScrollingSpace.gone()

        binding.jobDetailsAppliedDividerView.gone()
        binding.jobDetailsAppliedOnWithdrawnSpace.gone()
        binding.jobDetailsWithdrawTextView.gone()
        binding.jobDetailsContactTextView.gone()
        binding.jobDetailsAppliedJobGroup.gone()
        binding.notEligibleTextView.gone()
        binding.applyButton.visible()

        binding.applyButton.text = when (applyForJobUiState) {
            is ApplyForJobUiState.ReadyToApply -> when (applyForJobUiState) {
                ApplyForJobUiState.ReadyToApply.External -> getString(R.string.applyButtonApplyOnExternalSite)
                ApplyForJobUiState.ReadyToApply.Internal -> getString(R.string.applyButtonReadyToApply)
            }
            ApplyForJobUiState.Ended -> getString(R.string.applyButtonJobEnded)
            ApplyForJobUiState.Offline -> getString(R.string.applyButtonOffline)
            is ApplyForJobUiState.NotEligibleToApply -> error("this case is handled by the outer when statement")
            is ApplyForJobUiState.AppliedOn -> error("this case is handled by the outer when statement")
            is ApplyForJobUiState.Withdrawn -> error("this case is handled by the outer when statement")
        }
    }

    private fun showWithdrawnJobUI(applyForJobUiState: ApplyForJobUiState.Withdrawn) {
        binding.jobDetailsAppliedJobGroup.visible()
        binding.jobDetailsAppliedOnTextView.text = getString(R.string.withdrawnJobText)
        setApplicationStatus(ApplicationStatus.WITHDRAWN)
        binding.applyButton.gone()

        binding.jobDetailsContentRoot.saveButton.gone()
        binding.jobDetailsContentRoot.hideButton.gone()
        binding.notEligibleTextView.gone()
        binding.jobDetailsContentRoot.scrollingSpace.gone()
        binding.jobDetailsContentRoot.jobDetailsAppliedJobScrollingSpace.visible()

        binding.jobDetailsAppliedDividerView.visible()
        binding.jobDetailsAppliedOnWithdrawnSpace.gone()
        binding.jobDetailsWithdrawTextView.gone()
        binding.jobDetailsContactTextView.visible()

        binding.jobDetailsContactTextView.setOnClickListener {
            logAnalyticsEvent(AnalyticEvents.JobDetails.CONTACT_RECRUITER_TAPPED, AnalyticsEventType.TAP)
            openEmail(applyForJobUiState.emailForApplications)
        }
    }

    private fun showLoading() {
        binding.jobDetailsContent.visibility = View.GONE
        binding.setupJobAlertLoadingIndicator.visibility = View.VISIBLE
        binding.loadingError.visibility = View.GONE
        binding.applyButton.visibility = View.INVISIBLE
    }

    private fun showError() {
        binding.jobDetailsContent.visibility = View.GONE
        binding.setupJobAlertLoadingIndicator.visibility = View.GONE
        binding.applyButton.visibility = View.INVISIBLE
        binding.loadingError.visibility = View.VISIBLE
    }

    private fun setupBrandedJobAppearance(appearance: BrandedJobAppearance) {
        when (appearance) {
            is BrandedJobAppearance.ImageOnly -> showBrandedImage(appearance.image, calculateBrandedColor = true)
            is BrandedJobAppearance.ColorOnly -> {
                showBrandedImageColorOnly(appearance.color)
            }
            is BrandedJobAppearance.ImageAndColor -> {
                showBrandedImage(appearance.image, calculateBrandedColor = false)
                setBrandedColor(appearance.color)
            }
            is BrandedJobAppearance.DefaultImageAndColor -> {
                showBrandedImageColorOnly(resources.getColor(R.color.brand_03_50_brand_01_80, null))
            }
        }
    }

    private fun showBrandedMediaContent(content: List<BrandedJobMediaItem>, companyName: String) {
        binding.jobDetailsContentRoot.brandedMediaHeader.text = getString(R.string.brandedContentHeader, companyName)
        binding.jobDetailsContentRoot.brandedMediaGroup.visibleOrGone(content.isNotEmpty())
        if (content.isNotEmpty()) {
            val brandedMediaAdapter = BrandedMediaAdapter(imageLoader) { mediaItem ->
                logBrandedMediaTappedAnalyticsEvent(mediaItem)

                if (mediaItem.videoUrl != null) {
                    openUri(Uri.parse(mediaItem.videoUrl))
                }
            }
            brandedMediaAdapter.items = content
            binding.jobDetailsContentRoot.brandedMedia.apply {
                adapter = brandedMediaAdapter
                layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                setHasFixedSize(true)
            }
        }
    }

    private fun logBrandedMediaTappedAnalyticsEvent(mediaItem: BrandedJobMediaItem) {
        val mediaItemNumber = (binding.jobDetailsContentRoot.brandedMedia.adapter as BrandedMediaAdapter).items.indexOf(mediaItem) + 1
        val mediaItemNumberBundle = mapOf(AnalyticEvents.JobDetails.IMAGE_CAROUSEL_IMAGE_NUMBER_PARAM to mediaItemNumber)
        logAnalyticsEvent(AnalyticEvents.JobDetails.MEDIA_CAROUSEL_TAP, AnalyticsEventType.TAP, mediaItemNumberBundle)
    }

    private fun showBrandedImageColorOnly(@ColorInt backgroundColor: Int) {
        binding.appBar.addOnOffsetChangedListener(FadeInAfterToolbarCollapse(binding.toolbar, binding.toolbar))
        binding.brandedBackButton.visible()
        binding.brandedJobImageGradient.visibility = View.GONE

        binding.brandedJobImage.setImageResource(R.drawable.ic_branded_job_banner)
        setCardBackground(backgroundColor)
        binding.collapsingToolbarLayout.setBackgroundColor(backgroundColor)
    }

    private fun showBrandedImage(imageUrl: String, calculateBrandedColor: Boolean) {
        binding.appBar.addOnOffsetChangedListener(FadeInAfterToolbarCollapse(binding.toolbar, binding.toolbar))
        binding.brandedBackButton.visible()

        imageLoader.load(binding.brandedJobImage) {
            asBitmap()
                .load(imageUrl)
                .centerCrop()
                .error(R.drawable.ic_branded_job_banner)
                .apply {
                    if (calculateBrandedColor) {
                        onResourceReady { bitmap, _, _, _, _ ->
                            setBrandedBackgroundBasedOnImage(bitmap)
                        }
                    }
                }
        }
    }

    private fun setBrandedBackgroundBasedOnImage(bitmap: Bitmap) {
        viewScope.launch {
            val calculatedBrandedColor = withContext(computationDispatcher) {
                calculateAverageColorFromBrandedImage(bitmap)
            }
            setBrandedColor(calculatedBrandedColor)
        }
    }

    private fun setBrandedColor(@ColorInt color: Int) {
        setCardBackground(color)
        binding.brandedJobImageGradient.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.TRANSPARENT, color)
        )
    }

    private fun setCardBackground(@ColorInt color: Int) {
        val cardBackground = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(color, resources.getColor(R.color.neutrals_50_neutrals_130, null))
        )
        binding.jobDetailsContentRoot.cardViewContentBackground.background = cardBackground
    }

    private fun showJobDetails(details: Job) {
        binding.jobDetailsContent.visibility = View.VISIBLE
        binding.applyButton.visibility = View.VISIBLE
        binding.setupJobAlertLoadingIndicator.visibility = View.GONE
        binding.loadingError.visibility = View.GONE

        binding.toolbarTitle.text = details.jobTitle
        binding.appBar.addOnOffsetChangedListener(FadeInAfterToolbarCollapse(binding.toolbar, binding.toolbarTitle))

        imageLoader.load(binding.jobDetailsContentRoot.logoImage) {
            loadJobLogo(
                logoUrl = details.logoLink,
                container = binding.jobDetailsContentRoot.logoImageContainer
            )
        }

        binding.jobDetailsContentRoot.jobDetailsJobTitle.text = details.jobTitle
        binding.jobDetailsContentRoot.companyNameText.text = details.companyName

        binding.jobDetailsContentRoot.location.text = details.displayLocation
        binding.jobDetailsContentRoot.salaryText.text = details.displaySalary
        binding.jobDetailsContentRoot.jobTypeText.text = requireContext().formatJobType(
            details.jobType,
            mapEmploymentForm(details),
            true
        )
        setPostedDaysCount(details)

        setJobDescription(details)
        setSkills(details.skills)

        setApplicationStatus(details.applicationStatus)
        setTextFooterStatusDate(details.applicationStatus, details.applicationStatusUpdatedOn)

        showBrandedMediaContent(details.brandedDetails?.content.orEmpty(), details.companyName)
        setupBrandedRelatedAppearance(details)

        binding.jobDetailsContentRoot.referenceText.text = getString(R.string.jobDetailsReferenceText, details.jobId)

        applyStateForSavedButton(details)
        applyStateForHideButton(details)

        binding.jobDetailsContentRoot.shareButton.setOnClickListener {
            logAnalyticsEvent(AnalyticEvents.JobDetails.SHARE_JOB_TAPPED, AnalyticsEventType.TAP)
            viewModel.shareJob(details)
        }
    }

    private fun applyStateForSavedButton(details: Job) {
        if (details.jobState == JobState.SAVED) {
            with(binding.jobDetailsContentRoot.saveButton) {
                setBackgroundResource(R.drawable.bg_job_action_button_saved)
                setTextColor(resources.getColor(R.color.neutrals_40, null))
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    resources.getDrawable(R.drawable.ic_heart_filled, null),
                    null,
                    null
                )
                compoundDrawableTintList = ColorStateList.valueOf(resources.getColor(R.color.neutrals_40, null))
                text = getText(R.string.saved)
            }
        } else {
            with(binding.jobDetailsContentRoot.saveButton) {
                setBackgroundResource(R.drawable.bg_job_action_button)
                setTextColor(resources.getColor(R.color.neutrals_130_neutrals_40, null))
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    resources.getDrawable(R.drawable.ic_heart, null),
                    null,
                    null
                )
                compoundDrawableTintList = ColorStateList.valueOf(resources.getColor(R.color.neutrals_130_neutrals_40, null))
                text = getText(R.string.save)
            }
        }
    }

    private fun applyStateForHideButton(details: Job) {
        if (details.jobState == JobState.HIDDEN) {
            val textColor = resources.getColor(R.color.neutrals_40_neutrals_130, null)
            with(binding.jobDetailsContentRoot.hideButton) {
                setBackgroundResource(R.drawable.bg_job_action_button_hide)
                setTextColor(textColor)
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    resources.getDrawable(R.drawable.ic_eye, null),
                    null,
                    null
                )
                compoundDrawableTintList = ColorStateList.valueOf(textColor)
                text = getText(R.string.unhide)
            }
        } else {
            val textColor = resources.getColor(R.color.neutrals_130_neutrals_40, null)
            with(binding.jobDetailsContentRoot.hideButton) {
                setBackgroundResource(R.drawable.bg_job_action_button)
                setTextColor(textColor)
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    resources.getDrawable(R.drawable.ic_eye_off, null),
                    null,
                    null
                )
                compoundDrawableTintList = ColorStateList.valueOf(textColor)
                text = getText(R.string.hide)
            }
        }
    }

    private fun setupBrandedRelatedAppearance(details: Job) {
        if (details.brandedDetails?.appearance != null) {
            setupBrandedJobAppearance(details.brandedDetails.appearance)
        } else {
            setNotBrandedToolbarHeight()
        }
    }

    private fun setPostedDaysCount(details: Job) {
        val postedDaysCount = details.jobPostedDaysCount
        if (postedDaysCount != null) {
            binding.jobDetailsContentRoot.postedOnText.visible()
            binding.jobDetailsContentRoot.postedOnText.text = requireContext().formatDayPosted(postedDaysCount)
        } else {
            binding.jobDetailsContentRoot.postedOnText.gone()
        }
    }

    private fun setJobDescription(details: Job) {
        val htmlSpannable = HtmlCompat.fromHtml(details.description, FROM_HTML_MODE_LEGACY or FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM)
        binding.jobDetailsContentRoot.jobDescription.text = htmlSpannable.fixBulletSpans().trim()

        viewScope.launch {
            binding.jobDetailsContentRoot.jobDescription.waitForNextGlobalLayout()
            if (!binding.jobDetailsContentRoot.jobDescription.canBeExpanded) {
                binding.jobDetailsContentRoot.collapsedTextCover.gone()
                binding.jobDetailsContentRoot.toggleTextCollapsingButton.gone()
            }
        }
        if (binding.jobDetailsContentRoot.jobDescription.isExpanded) {
            binding.jobDetailsContentRoot.collapsedTextCover.gone()
            jobDescriptionShowLessState()
        }
        binding.jobDetailsContentRoot.jobDescription.onExpandListener = object : ExpandableTextView.SimpleOnExpandListener() {
            override fun onExpand(view: ExpandableTextView) {
                jobDescriptionShowLessState()
                logAnalyticsEvent(AnalyticEvents.JobDetails.READ_MORE_TAPPED, AnalyticsEventType.TAP)
            }

            override fun onExpanded(view: ExpandableTextView) {
                binding.jobDetailsContentRoot.collapsedTextCover.gone()
            }

            override fun onCollapse(view: ExpandableTextView) {
                binding.jobDetailsContentRoot.collapsedTextCover.visible()
                jobDescriptionShowMoreState()
                logAnalyticsEvent(AnalyticEvents.JobDetails.SHOW_LESS_TAPPED, AnalyticsEventType.TAP)
            }
        }
        binding.jobDetailsContentRoot.toggleTextCollapsingButton.setOnClickListener {
            binding.jobDetailsContentRoot.jobDescription.toggle()
        }
    }

    private fun setNotBrandedToolbarHeight() {
        val layoutParams = binding.appBar.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.height = resources.getDimension(R.dimen.jobDetailsNotBrandedExpandedToolbarHeight).toInt()
        binding.appBar.layoutParams = layoutParams
    }

    private fun setSkills(skills: List<String>) {
        if (skills.isEmpty()) {
            binding.jobDetailsContentRoot.skillsGroup.visibility = View.GONE
            return
        }
        binding.jobDetailsContentRoot.skillsGroup.visibility = View.VISIBLE
        binding.jobDetailsContentRoot.skillsContainer.removeAllViews()
        skills.forEach { skill ->
            val skillView = layoutInflater.inflate(R.layout.view_job_details_skill, binding.jobDetailsContentRoot.skillsContainer, false) as Chip
            skillView.text = skill
            binding.jobDetailsContentRoot.skillsContainer.addView(skillView)
        }
    }

    private fun showLocationOnMap(googleMap: GoogleMap, location: LatLng) {
        googleMap.clear()
        googleMap.uiSettings!.isZoomControlsEnabled = true
        googleMap.addCircle(
            CircleOptions()
                .center(location)
                .radius(1600.0)
                .strokeWidth(resources.getDimension(R.dimen.jobDetailsMapCircleStrokeWidth))
                .strokeColor(resources.getColor(R.color.mapAreaColor, null))
                .fillColor(resources.getColor(R.color.mapAreaColor, null))
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))
        googleMap.setOnMapClickListener {
            logAnalyticsEvent(AnalyticEvents.JobDetails.MAP_TAPPED, AnalyticsEventType.TAP)

            viewModel.mapClicked(location)
        }
    }

    private fun openMapWithOriginPoint(location: LatLng, postcode: String) {
        val uri = "https://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}&origin=$postcode"
        showOpenMapDialog(uri)
    }

    private fun openMapWithoutOriginPoint(location: LatLng) {
        val uri = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
        showOpenMapDialog(uri)
    }

    private fun showOpenMapDialog(uri: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.jobDetailsMapConfirmationPopup))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val gmmIntentUri: Uri = Uri.parse(uri)
                openUri(gmmIntentUri)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun observeAuth() {
        viewModel.authenticationResultEvent.handle(viewLifecycleOwner) {
            exaustive - when (it) {
                is AuthViewModel.AuthenticationResult.Success -> {
                    trackSignInMethod(it.authMethod, AnalyticsScreenNames.APPLY_WELCOME_VIEW)
                    viewModel.applyForAJob()
                }
                is AuthViewModel.AuthenticationResult.PostRegistrationRequired -> {
                    findNavController().navigateSafe(
                        R.id.action_jobDetailsFragment_to_profilePopupFragment,
                        PostRegistrationProfileFragmentArgs(
                            PostRegistrationProfileState.FillUpProfileWithPostRegistration(
                                it.postRegistrationData
                            )
                        ).toBundle()
                    )
                }
                AuthViewModel.AuthenticationResult.Failure.NetworkError -> showOfflineSnackbar(
                    requireView(),
                    binding.setupJobAlertToastAnchor
                )
                AuthViewModel.AuthenticationResult.Failure.OtherError -> showSomethingWentWrongSnackbar(
                    requireView(),
                    binding.setupJobAlertToastAnchor
                )
                AuthViewModel.AuthenticationResult.Failure.CancelledByUser -> {
                    logAnalyticsEvent(AnalyticEvents.Authentication.AUTH_CANCELLED_BY_USER, AnalyticsEventType.KEY)
                }
            }
        }
    }

    private fun Spanned.fixBulletSpans(): SpannableStringBuilder {
        val spannableBuilder = SpannableStringBuilder(this)

        val bulletSpans = spannableBuilder.getSpans(0, spannableBuilder.length, BulletSpan::class.java)
        bulletSpans.forEach {
            val start = spannableBuilder.getSpanStart(it)
            val end = spannableBuilder.getSpanEnd(it)
            spannableBuilder.removeSpan(it)

            val bulletRadius = resources.getDimensionPixelSize(R.dimen.jobDetailsBulletRadius)
            val bulletGap = resources.getDimensionPixelSize(R.dimen.jobDetailsBulletGap)
            spannableBuilder.setSpan(
                ImprovedBulletSpan(bulletRadius = bulletRadius, gapWidth = bulletGap),
                start,
                end,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        return spannableBuilder
    }
}
