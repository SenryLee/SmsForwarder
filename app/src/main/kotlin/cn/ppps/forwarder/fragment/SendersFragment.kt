package cn.ppps.forwarder.fragment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.alibaba.android.vlayout.VirtualLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import cn.ppps.forwarder.R
import cn.ppps.forwarder.activity.MainActivity
import cn.ppps.forwarder.adapter.SenderPagingAdapter
import cn.ppps.forwarder.adapter.WidgetItemAdapter
import cn.ppps.forwarder.core.BaseFragment
import cn.ppps.forwarder.database.entity.Sender
import cn.ppps.forwarder.database.viewmodel.BaseViewModelFactory
import cn.ppps.forwarder.database.viewmodel.SenderViewModel
import cn.ppps.forwarder.databinding.FragmentSendersBinding
import cn.ppps.forwarder.fragment.senders.EmailFragment
import cn.ppps.forwarder.fragment.senders.WebhookFragment
import cn.ppps.forwarder.utils.KEY_SENDER_CLONE
import cn.ppps.forwarder.utils.KEY_SENDER_ID
import cn.ppps.forwarder.utils.KEY_SENDER_TYPE
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.TYPE_EMAIL
import cn.ppps.forwarder.utils.TYPE_WEBHOOK
import cn.ppps.forwarder.utils.XToastUtils
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.base.XPageFragment
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xpage.enums.CoreAnim
import com.xuexiang.xpage.model.PageInfo
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder
import com.xuexiang.xui.utils.DensityUtils
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.alpha.XUIAlphaTextView
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.resource.ResUtils.getStringArray
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Suppress("PrivatePropertyName", "DEPRECATION")
@Page(name = "发送通道")
class SendersFragment : BaseFragment<FragmentSendersBinding?>(),
    SenderPagingAdapter.OnItemClickListener,
    RecyclerViewHolder.OnItemClickListener<PageInfo> {

    private val TAG: String = SendersFragment::class.java.simpleName
    private val that = this
    private var titleBar: TitleBar? = null
    private var adapter = SenderPagingAdapter(this)
    private val viewModel by viewModels<SenderViewModel> { BaseViewModelFactory(context) }
    private val dialog: BottomSheetDialog by lazy { BottomSheetDialog(requireContext()) }
    private var currentStatus: Int = 1
    // 法院轻量版仅保留邮箱与 Webhook；params 中写入真实 type id
    private val SENDER_FRAGMENT_LIST = listOf(
        PageInfo(
            getString(R.string.email),
            "cn.ppps.forwarder.fragment.senders.EmailFragment",
            "{\"type\":$TYPE_EMAIL}",
            CoreAnim.slide,
            R.drawable.icon_email
        ),
        PageInfo(
            getString(R.string.webhook),
            "cn.ppps.forwarder.fragment.senders.WebhookFragment",
            "{\"type\":$TYPE_WEBHOOK}",
            CoreAnim.slide,
            R.drawable.icon_webhook
        ),
    )

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentSendersBinding {
        return FragmentSendersBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setLeftImageResource(R.drawable.ic_action_menu)
        titleBar!!.setTitle(R.string.menu_senders)
        titleBar!!.setLeftClickListener { getContainer()?.openMenu() }
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_add) {
            @SuppressLint("InflateParams")
            @SingleClick
            override fun performAction(view: View) {
                val bottomSheet: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sender_bottom_sheet, null)
                val recyclerView: RecyclerView = bottomSheet.findViewById(R.id.recyclerView)

                WidgetUtils.initGridRecyclerView(recyclerView, 4, DensityUtils.dp2px(1f))
                val widgetItemAdapter = WidgetItemAdapter(SENDER_FRAGMENT_LIST)
                widgetItemAdapter.setOnItemClickListener(that)
                recyclerView.adapter = widgetItemAdapter

                val bottomSheetCloseButton: XUIAlphaTextView = bottomSheet.findViewById(R.id.bottom_sheet_close_button)
                bottomSheetCloseButton.setOnClickListener { dialog.dismiss() }

                dialog.setContentView(bottomSheet)
                dialog.setCancelable(true)
                dialog.setCanceledOnTouchOutside(true)
                dialog.show()
                WidgetUtils.transparentBottomSheetDialogBackground(dialog)
            }
        })
        return titleBar
    }

    private fun getContainer(): MainActivity? {
        return activity as MainActivity?
    }

    override fun initViews() {
        val virtualLayoutManager = VirtualLayoutManager(requireContext())
        binding!!.recyclerView.layoutManager = virtualLayoutManager
        val viewPool = RecycledViewPool()
        binding!!.recyclerView.setRecycledViewPool(viewPool)
        viewPool.setMaxRecycledViews(0, 10)

        binding!!.tabBar.setTabTitles(getStringArray(R.array.status_param_option))
        binding!!.tabBar.setOnTabClickListener { _, position ->
            currentStatus = 1 - position
            viewModel.setStatus(currentStatus)
            adapter.refresh()
            binding!!.recyclerView.scrollToPosition(0)
        }
    }

    override fun initListeners() {
        binding!!.recyclerView.adapter = adapter

        binding!!.refreshLayout.setOnRefreshListener { refreshLayout: RefreshLayout ->
            refreshLayout.layout.postDelayed({
                lifecycleScope.launch {
                    viewModel.setStatus(currentStatus).allSenders.collectLatest { adapter.submitData(it) }
                }
                refreshLayout.finishRefresh()
            }, 200)
        }

        binding!!.refreshLayout.autoRefresh()
    }

    override fun onItemClicked(view: View?, item: Sender) {
        Log.e(TAG, item.toString())
        when (view?.id) {
            R.id.iv_copy -> {
                PageOption.to(getFragment(item.type))
                    .setNewActivity(true)
                    .putLong(KEY_SENDER_ID, item.id)
                    .putInt(KEY_SENDER_TYPE, item.type)
                    .putBoolean(KEY_SENDER_CLONE, true)
                    .open(this)
            }

            R.id.iv_edit -> {
                PageOption.to(getFragment(item.type))
                    .setNewActivity(true)
                    .putLong(KEY_SENDER_ID, item.id)
                    .putInt(KEY_SENDER_TYPE, item.type)
                    .open(this)
            }

            R.id.iv_delete -> {
                MaterialDialog.Builder(requireContext())
                    .title(R.string.delete_sender_title)
                    .content(R.string.delete_sender_tips)
                    .positiveText(R.string.lab_yes)
                    .negativeText(R.string.lab_no)
                    .onPositive { _: MaterialDialog?, _: DialogAction? ->
                        viewModel.delete(item.id)
                        XToastUtils.success(R.string.delete_sender_toast)
                    }
                    .show()
            }

            else -> {}
        }
    }

    override fun onItemRemove(view: View?, id: Int) {}

    @SingleClick
    override fun onItemClick(itemView: View, widgetInfo: PageInfo, pos: Int) {
        try {
            val senderType = when {
                widgetInfo.classPath.contains("EmailFragment") -> TYPE_EMAIL
                widgetInfo.classPath.contains("WebhookFragment") -> TYPE_WEBHOOK
                else -> TYPE_EMAIL
            }
            @Suppress("UNCHECKED_CAST")
            PageOption.to(Class.forName(widgetInfo.classPath) as Class<XPageFragment>)
                .setNewActivity(true)
                .putInt(KEY_SENDER_TYPE, senderType)
                .open(this)
            dialog.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "onItemClick error: ${e.message}")
            XToastUtils.error(e.message.toString())
        }
    }

    private fun getFragment(type: Int): Class<out XPageFragment> {
        return when (type) {
            TYPE_EMAIL -> EmailFragment::class.java
            TYPE_WEBHOOK -> WebhookFragment::class.java
            else -> EmailFragment::class.java
        }
    }

}
