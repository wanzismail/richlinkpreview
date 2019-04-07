package com.qkopy.richlink

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.qkopy.richlink.data.database.MetaDatabase
import com.qkopy.richlink.data.model.MetaData
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


open class RichLinkViewQkopy : RelativeLayout {

    private var view: View? = null
    internal var context: Context
    var metaData: MetaData? = null

    private lateinit var linearLayout: LinearLayout
    private lateinit var imageView: ImageView
    private lateinit var textViewTitle: TextView
    private lateinit var textViewDesp: TextView
    internal var textViewUrl: TextView? = null

    private var mainUrl: String? = null

    private var isDefaultClick = true

    private var richLinkListener: RichLinkListener? = null


    constructor(context: Context) : super(context) {
        this.context = context
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.context = context
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        this.context = context
    }


    fun initView() {

        if (findLinearLayoutChild() != null) {
            this.view = findLinearLayoutChild()
        } else {
            this.view = this
            View.inflate(context, R.layout.qkopy_link_layout, this)
        }




        linearLayout = findViewById<View>(R.id.rich_link_card) as LinearLayout
        imageView = findViewById<View>(R.id.rich_link_image) as ImageView
        textViewTitle = findViewById<View>(R.id.rich_link_title) as TextView
        textViewDesp = findViewById<View>(R.id.rich_link_desp) as TextView


        if (metaData!!.url == "" || metaData!!.url.isEmpty()) {
            imageView.visibility = View.GONE
        } else {
            imageView.visibility = View.VISIBLE
            Glide.with(context).load(metaData!!.image).into(imageView)
        }

        if (metaData!!.title.isEmpty() || metaData!!.title == "") {
            textViewTitle.visibility = View.GONE
        } else {
            textViewTitle.visibility = View.VISIBLE
            textViewTitle.text = metaData!!.title
        }

//        if (metaData!!.description.isEmpty() || metaData!!.description == "") {
//            textViewDesp.visibility = View.GONE
//        } else {
//            textViewDesp.visibility = View.VISIBLE
//            textViewDesp.text = metaData!!.description
//        }

        if (metaData!!.url.isEmpty() || metaData!!.url == "") {
            textViewDesp.visibility = View.GONE
        } else {
            textViewDesp.visibility = View.VISIBLE
            textViewDesp.text = metaData!!.url
        }


        linearLayout.setOnClickListener { view ->
            if (isDefaultClick) {
                richLinkClicked()
            } else {
                if (richLinkListener != null) {
                    richLinkListener!!.onClicked(view, metaData!!)
                } else {
                    richLinkClicked()
                }
            }
        }

    }


    private fun richLinkClicked() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mainUrl))
        context.startActivity(intent)
    }

    private fun findLinearLayoutChild(): LinearLayout? {
        return if (childCount > 0 && getChildAt(0) is LinearLayout) {
            getChildAt(0) as LinearLayout
        } else null
    }

    fun setLinkFromMeta(metaData: MetaData) {
        this.metaData = metaData
        initView()
    }


    fun setDefaultClickListener(isDefault: Boolean) {
        isDefaultClick = isDefault
    }

    fun setClickListener(richLinkListener1: RichLinkListener) {
        richLinkListener = richLinkListener1
    }

    fun setLink(url: String, viewListener: ViewListener) {
        mainUrl = url

        if (!mainUrl.isNullOrEmpty()) {
            val metaDataBase = MetaDatabase.getInstance(context)
            doAsync {
                println("meta for mainUrl $url")
                val meta = metaDataBase.metaDataDao().getMetaDataUrl(url)
                uiThread {
                    if (meta != null) {
                        println("meta from local db")
                        metaData = meta
                        initView()
                    } else {
                        val richPreview = RichPreview(object : ResponseListener {
                            override fun onData(meta: MetaData?) {
                                metaData = meta
                                if (metaData?.title?.isEmpty() == false || metaData?.title == "") {
                                    viewListener.onSuccess(true)
                                    println("meta from local response")
                                    doAsync {
                                        println("meta insert to db ${metaData?.url}")
                                        metaDataBase.metaDataDao().insert(metaData!!)
                                    }
                                    initView()
                                }
                            }

                            override fun onError(e: Exception) {
                                viewListener.onError(e)
                            }
                        })

                        richPreview.getPreview(url)
                    }
                }
            }

        }

    }


}
