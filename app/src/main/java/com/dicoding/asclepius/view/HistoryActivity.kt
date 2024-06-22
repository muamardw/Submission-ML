package com.dicoding.asclepius.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityHistoryBinding
import com.dicoding.asclepius.history.HistoryAdapter
import com.dicoding.asclepius.history.HistoryDatabase
import com.dicoding.asclepius.history.HistoryPrediction
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity(), HistoryAdapter.OnDeleteClickListener {
    private lateinit var adapter: HistoryAdapter
    private var historyList: MutableList<HistoryPrediction> = mutableListOf()
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var binding: ActivityHistoryBinding

    companion object {
        private const val TAG = "HistoryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigationView()
        setupRecyclerView()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch(Dispatchers.Main) {
            loadPredictionHistory()
        }
    }

    private fun setupBottomNavigationView() {
        bottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    navigateToHome()
                    true
                }
                R.id.history -> true
                else -> false
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(historyList)
        adapter.setOnDeleteClickListener(this)
        binding.rvHistory.adapter = adapter
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ResultActivity.REQUEST_HISTORY_UPDATE && resultCode == RESULT_OK) {
            lifecycleScope.launch(Dispatchers.Main) {
                loadPredictionHistory()
            }
        }
    }

    private suspend fun loadPredictionHistory() {
        val predictionDao = HistoryDatabase.getInstance(this).predictionHistoryDao()
        val predictions = withContext(Dispatchers.IO) {
            predictionDao.getAllHistories()
        }
        Log.d(TAG, "Number of predictions: ${predictions.size}")
        historyList.apply {
            clear()
            addAll(predictions)
        }
        adapter.notifyDataSetChanged()
        updateViewVisibility()
    }

    private fun updateViewVisibility() {
        with(binding) {
            tvNotFound.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
            rvHistory.visibility = if (historyList.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onDeleteClick(position: Int) {
        val prediction = historyList[position]
        lifecycleScope.launch(Dispatchers.IO) {
            HistoryDatabase.getInstance(this@HistoryActivity).predictionHistoryDao().deleteHistories(prediction)
            withContext(Dispatchers.Main) {
                historyList.removeAt(position)
                adapter.notifyDataSetChanged()
                updateViewVisibility()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bottom_navi_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
