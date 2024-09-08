package com.rk.xededitor.MainActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.rk.xededitor.BaseActivity
import com.rk.xededitor.MainActivity.StaticData.fileSet
import com.rk.xededitor.MainActivity.StaticData.fragments
import com.rk.xededitor.MainActivity.StaticData.mTabLayout
import com.rk.xededitor.MainActivity.editor.AutoSaver
import com.rk.xededitor.MainActivity.editor.DynamicFragment
import com.rk.xededitor.MainActivity.editor.NoSwipeViewPager
import com.rk.xededitor.MainActivity.editor.TabAdapter
import com.rk.xededitor.MainActivity.file.FileAction
import com.rk.xededitor.MainActivity.file.FileManager
import com.rk.xededitor.MainActivity.file.ProjectManager
import com.rk.xededitor.MainActivity.handlers.MenuClickHandler.handle
import com.rk.xededitor.MainActivity.handlers.MenuItemHandler
import com.rk.xededitor.MainActivity.handlers.PermissionHandler

import com.rk.xededitor.R
import com.rk.xededitor.Settings.SettingsData
import com.rk.xededitor.databinding.ActivityMainBinding
import com.rk.xededitor.rkUtils
import java.io.File

class MainActivity : BaseActivity() {

    lateinit var binding: ActivityMainBinding
    var adapter: TabAdapter? = null
    var viewPager: NoSwipeViewPager? = null
    var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var drawerToggle: ActionBarDrawerToggle? = null

    var isPaused = false

    override fun onPause() {
        isPaused = true
        super.onPause()
    }



    override fun onResume() {
        isPaused = false
        ProjectManager.processQueue(this)
        //restart auto saver if it stopped
        AutoSaver.start(this)
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        StaticData.clear()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        setupTheme()
        setupDrawer()
        initiateStaticVariables()
        ActivitySetup.init(this)

        ProjectManager.restoreProjects(this)

    }

    private fun setupTheme() {
        if (SettingsData.isDarkMode(this)) {
            if (SettingsData.isOled()) {
                val black = Color.BLACK
                with(binding) {
                    drawerLayout.setBackgroundColor(black)
                    navView.setBackgroundColor(black)
                    main.setBackgroundColor(black)
                    appbar.setBackgroundColor(black)
                    toolbar.setBackgroundColor(black)
                    tabs.setBackgroundColor(black)
                    mainView.setBackgroundColor(black)
                }
            } else {
                val window = window
                window.navigationBarColor = Color.parseColor("#141118")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }


    private fun setupDrawer() {
        drawerLayout = binding.drawerLayout
        navigationView = binding.navView
        navigationView?.layoutParams?.width =
            (Resources.getSystem().displayMetrics.widthPixels * 0.87).toInt()

        drawerToggle =
            ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer)
        drawerLayout?.addDrawerListener(drawerToggle!!)
        drawerToggle?.syncState()
        drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

    }

    private fun initiateStaticVariables() {
        viewPager = binding.viewpager
        mTabLayout = binding.tabs
        viewPager!!.setOffscreenPageLimit(15)
        mTabLayout.setupWithViewPager(viewPager)

        if (adapter == null) {
            fragments = ArrayList()
            adapter = TabAdapter(supportFragmentManager)
            viewPager!!.setAdapter(adapter)
            fileSet = HashSet()
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                StaticData.MANAGE_EXTERNAL_STORAGE -> {
                    PermissionHandler.verifyStoragePermission(this)
                }

                FileAction.REQUEST_CODE_OPEN_DIRECTORY -> {
                    FileManager.handleOpenDirectory(data, this)
                }

                FileAction.REQUEST_ADD_FILE -> {
                    FileManager.handleAddFile(data, this)
                }

                StaticData.REQUEST_FILE_SELECTION -> {
                    data?.let { FileManager.handleFileSelection(it, this) }

                }

                StaticData.REQUEST_DIRECTORY_SELECTION -> {
                    data?.let { FileManager.handleDirectorySelection(it, this) }
                }

            }
        }
    }


    fun newEditor(file: File, text: String? = null) {
        if (fileSet.contains(file)) {
            rkUtils.toast(this, "File already opened!")
            return
        }
        fileSet.add(file)
        val fragment = DynamicFragment(file)
        text?.let { fragment.editor.setText(it) }
        adapter!!.addFragment(fragment, file)
        for (i in 0 until mTabLayout.tabCount) {
            mTabLayout.getTabAt(i)?.setText(fragments[i].fileName)
        }
        MenuItemHandler.updateMenuItems()
        AutoSaver.start(this)
    }


    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        StaticData.menu = menu

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        MenuItemHandler.updateMenuItems()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            with(drawerLayout!!) {
                val start = GravityCompat.START
                if (isDrawerOpen(start)) {
                    closeDrawer(start)
                } else {
                    openDrawer(start)
                }
            }
            return true
        } else {
            if (drawerToggle!!.onOptionsItemSelected(item)) {
                return true
            }
            return handle(this, item)
        }
    }


    override fun onDestroy() {
        StaticData.clear();super.onDestroy()
    }

    //view click listeners
    fun openFile(v: View?) {
        FileManager.openFile()
    }

    fun openDrawer(v: View?) {
        drawerLayout!!.open()
    }
}